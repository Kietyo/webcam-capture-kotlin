package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class is used as a global (system) lock preventing other processes from using the same
 * camera while it's open. Whenever webcam is open there is a thread running in background which
 * updates the lock once per 2 seconds. Lock is being released whenever webcam is either closed or
 * completely disposed. Lock will remain for at least 2 seconds in case when JVM has not been
 * gracefully terminated (due to SIGSEGV, SIGTERM, etc).
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamLock(
    /**
     * And the Webcam we will be locking.
     */
    private val webcam: Webcam
) {
    /**
     * Used to update lock state.
     *
     * @author sarxos
     */
    private inner class LockUpdater : Thread() {
        init {
            name = String.format("webcam-lock-[%s]", webcam.name)
            isDaemon = true
            uncaughtExceptionHandler = WebcamExceptionHandler.instance
        }

        override fun run() {
            do {
                if (disabled.get()) {
                    return
                }
                update()
                try {
                    sleep(INTERVAL)
                } catch (e: InterruptedException) {
                    LOG.debug("Lock updater has been interrupted")
                    return
                }
            } while (locked.get())
        }
    }

    /**
     * Updater thread. It will update the lock value in fixed interval.
     */
    private var updater: Thread? = null

    /**
     * Is webcam locked (local, not cross-VM variable).
     */
    private val locked = AtomicBoolean(false)

    /**
     * Is lock completely disabled.
     */
    private val disabled = AtomicBoolean(false)

    /**
     * Lock file.
     */
    val lockFile: File

    /**
     * Creates global webcam lock.
     *
     * @param webcam the webcam instance to be locked
     */
    init {
        lockFile = File(System.getProperty("java.io.tmpdir"), lockName)
        lockFile.deleteOnExit()
    }

    private val lockName: String
        get() = String.format(".webcam-lock-%d", Math.abs(webcam.name.hashCode()))

    private fun write(value: Long) {
        if (disabled.get()) {
            return
        }
        val name = lockName
        var tmp: File? = null
        var dos: DataOutputStream? = null
        try {
            tmp = File.createTempFile(String.format("%s-tmp", name), "")
            tmp.deleteOnExit()
            dos = DataOutputStream(FileOutputStream(tmp))
            dos.writeLong(value)
            dos.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            if (dos != null) {
                try {
                    dos.close()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
        if (!locked.get()) {
            return
        }
        if (tmp!!.renameTo(lockFile)) {

            // atomic rename operation can fail (mostly on Windows), so we
            // simply jump out the method if it succeed, or try to rewrite
            // content using streams if it fail
            return
        } else {

            // create lock file if not exist
            if (!lockFile.exists()) {
                try {
                    if (lockFile.createNewFile()) {
                        LOG.info("Lock file {} for {} has been created", lockFile, webcam)
                    } else {
                        throw RuntimeException("Not able to create file " + lockFile)
                    }
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            var fos: FileOutputStream? = null
            var fis: FileInputStream? = null
            var k = 0
            var n = -1
            val buffer = ByteArray(8)
            var rewritten = false

            // rewrite temporary file content to lock, try max 5 times
            synchronized(webcam) {
                do {
                    try {
                        fos = FileOutputStream(lockFile)
                        fis = FileInputStream(tmp)
                        while (fis!!.read(buffer).also { n = it } != -1) {
                            fos!!.write(buffer, 0, n)
                        }
                        rewritten = true
                    } catch (e: IOException) {
                        LOG.debug("Not able to rewrite lock file", e)
                    } finally {
                        if (fos != null) {
                            try {
                                fos!!.close()
                            } catch (e: IOException) {
                                throw RuntimeException(e)
                            }
                        }
                        if (fis != null) {
                            try {
                                fis!!.close()
                            } catch (e: IOException) {
                                throw RuntimeException(e)
                            }
                        }
                    }
                    if (rewritten) {
                        break
                    }
                } while (k++ < 5)
            }
            if (!rewritten) {
                throw WebcamException("Not able to write lock file")
            }

            // remove temporary file
            if (!tmp.delete()) {
                tmp.deleteOnExit()
            }
        }
    }

    private fun read(): Long {
        if (disabled.get()) {
            return -1
        }
        var dis: DataInputStream? = null
        var value: Long = -1
        var broken = false
        synchronized(webcam) {
            try {
                value = DataInputStream(FileInputStream(lockFile)).also { dis = it }.readLong()
            } catch (e: EOFException) {
                LOG.debug("Webcam lock is broken - EOF when reading long variable from stream", e)
                broken = true
            } catch (e: IOException) {
                throw RuntimeException(e)
            } finally {
                if (dis != null) {
                    try {
                        dis!!.close()
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }
            if (broken) {
                LOG.warn("Lock file {} for {} is broken - recreating it", lockFile, webcam)
                write(-1)
            }
        }
        return value
    }

    private fun update() {
        if (disabled.get()) {
            return
        }
        write(System.currentTimeMillis())
    }

    /**
     * Lock webcam.
     */
    fun lock() {
        if (disabled.get()) {
            return
        }
        if (isLocked()) {
            throw WebcamLockException(String.format("Webcam %s has already been locked", webcam.name))
        }
        if (!locked.compareAndSet(false, true)) {
            return
        }
        LOG.debug("Lock {}", webcam)
        update()
        updater = LockUpdater()
        updater!!.start()
    }

    /**
     * Completely disable locking mechanism. After this method is invoked, the lock will not have
     * any effect on the webcam runtime.
     */
    fun disable() {
        if (disabled.compareAndSet(false, true)) {
            LOG.info("Locking mechanism has been disabled in {}", webcam)
            if (updater != null) {
                updater!!.interrupt()
            }
        }
    }

    /**
     * Unlock webcam.
     */
    fun unlock() {

        // do nothing when lock disabled
        if (disabled.get()) {
            return
        }
        if (!locked.compareAndSet(true, false)) {
            return
        }
        LOG.debug("Unlock {}", webcam)
        updater!!.interrupt()
        write(-1)
        if (!lockFile.delete()) {
            lockFile.deleteOnExit()
        }
    }

    /**
     * Check if webcam is locked.
     *
     * @return True if webcam is locked, false otherwise
     */
    fun isLocked(): Boolean {

        // always return false when lock is disabled
        if (disabled.get()) {
            return false
        }

        // check if locked by current process
        if (locked.get()) {
            return true
        }

        // check if locked by other process
        if (!lockFile.exists()) {
            return false
        }
        val now = System.currentTimeMillis()
        val tsp = read()
        LOG.trace("Lock timestamp {} now {} for {}", tsp, now, webcam)
        return if (tsp > now - INTERVAL * 2) {
            true
        } else false
    }

    companion object {
        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamLock::class.java)

        /**
         * Update interval (ms).
         */
        const val INTERVAL: Long = 2000
    }
}