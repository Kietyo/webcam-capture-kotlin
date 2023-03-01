package com.github.sarxos.webcam

import com.github.sarxos.webcam.Webcam.Companion.discoveryListeners
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WebcamDiscoveryService(private val driver: WebcamDriver) : Runnable {
    private class WebcamsDiscovery(private val driver: WebcamDriver) : Callable<List<Webcam>>, ThreadFactory {
        @Throws(Exception::class)
        override fun call(): List<Webcam> {
            return toWebcams(driver.devices)
        }

        override fun newThread(r: Runnable): Thread {
            val t = Thread(r, "webcam-discovery-service")
            t.isDaemon = true
            t.uncaughtExceptionHandler = WebcamExceptionHandler.instance
            return t
        }
    }

    private val support: WebcamDiscoverySupport? = (if (driver is WebcamDiscoverySupport) driver else null)

    @Volatile
    private var webcams: MutableList<Webcam>? = null
    private val running = AtomicBoolean(false)
    private val enabled = AtomicBoolean(true)
    private var runner: Thread? = null

    @Throws(TimeoutException::class)
    fun getWebcams(timeout: NonNegativeTimeout, timeUnit: TimeUnit): List<Webcam> {
        var tmp: List<Webcam>? = null
        synchronized(Webcam::class.java) {
            if (webcams == null) {
                val discovery = WebcamsDiscovery(driver)
                val executor = Executors.newSingleThreadExecutor(discovery)
                val future = executor.submit(discovery)
                executor.shutdown()
                try {
                    executor.awaitTermination(timeout.value, timeUnit)
                    if (future.isDone) {
                        webcams = future.get().toMutableList()
                    } else {
                        future.cancel(true)
                    }
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                } catch (e: ExecutionException) {
                    throw WebcamException(e)
                }
                if (webcams == null) {
                    throw TimeoutException(
                        String.format(
                            "Webcams discovery timeout (%d ms) has been exceeded",
                            timeout
                        )
                    )
                }
                tmp = ArrayList(webcams)
                if (Webcam.isHandleTermSignal) {
                    WebcamDeallocator.store(webcams!!.toTypedArray())
                }
            }
        }
        if (tmp != null) {
            val listeners = discoveryListeners
            for (webcam in tmp!!) {
                notifyWebcamFound(webcam, listeners)
            }
        }
        return Collections.unmodifiableList(webcams)
    }

    /**
     * Scan for newly added or already removed webcams.
     */
    private fun scan() {
        val listeners = discoveryListeners
        val tmpNewDevices = driver.devices
        val tmpOldDevices: List<WebcamDevice> = try {
            getDevices(getWebcams(NonNegativeTimeout.MAX, TimeUnit.MILLISECONDS))
        } catch (e: TimeoutException) {
            throw WebcamException(e)
        }

        // convert to linked list due to O(1) on remove operation on
        // iterator versus O(n) for the same operation in array list
        val oldDevices: MutableList<WebcamDevice> = LinkedList(tmpOldDevices)
        val newDevices: MutableList<WebcamDevice> = LinkedList(tmpNewDevices)
        val oldDeviceItr = oldDevices.iterator()
        var newDeviceItr: MutableIterator<WebcamDevice?>?

        // reduce lists
        while (oldDeviceItr.hasNext()) {
            val oldDevice = oldDeviceItr.next()
            newDeviceItr = newDevices.iterator()
            while (newDeviceItr.hasNext()) {
                val newDevice = newDeviceItr.next()

                // remove both elements, if device name is the same, which
                // actually means that device is exactly the same
                if (newDevice.name == oldDevice.name) {
                    newDeviceItr.remove()
                    oldDeviceItr.remove()
                    break
                }
            }
        }

        // if any left in old ones it means that devices has been removed
        if (oldDevices.size > 0) {
            val toBeNotified: MutableList<Webcam> = ArrayList()
            for (device in oldDevices) {
                for (webcam in webcams!!) {
                    if (webcam.getDevice().name == device.name) {
                        toBeNotified.add(webcam)
                        break
                    }
                }
            }
            setCurrentWebcams(tmpNewDevices)
            for (webcam in toBeNotified) {
                notifyWebcamGone(webcam, listeners)
                webcam.dispose()
            }
        }

        // if any left in new ones it means that devices has been added
        if (newDevices.size > 0) {
            setCurrentWebcams(tmpNewDevices)
            for (device in newDevices) {
                for (webcam in webcams!!) {
                    if (webcam.getDevice().name == device.name) {
                        notifyWebcamFound(webcam, listeners)
                        break
                    }
                }
            }
        }
    }

    override fun run() {

        // do not run if driver does not support discovery
        if (support == null) {
            return
        }
        if (!support.isScanPossible) {
            return
        }

        // wait initial time interval since devices has been initially
        // discovered
        val monitor = ReentrantLock()
        val condition = monitor.newCondition()
        do {
            monitor.withLock {
                try {
                    condition.await(support.scanInterval, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    return
                } catch (e: Exception) {
                    throw RuntimeException("Problem waiting on monitor", e)
                }
            }
            scan()
        } while (running.get())
        LOG.debug("Webcam discovery service loop has been stopped")
    }

    private fun setCurrentWebcams(devices: List<WebcamDevice>) {
        webcams = toWebcams(devices)
        if (Webcam.isHandleTermSignal) {
            WebcamDeallocator.unstore()
            WebcamDeallocator.store(webcams!!.toTypedArray())
        }
    }

    /**
     * Stop discovery service.
     */
    fun stop() {

        // return if not running
        if (!running.compareAndSet(true, false)) {
            return
        }
        try {
            runner!!.join()
        } catch (e: InterruptedException) {
            throw WebcamException("Joint interrupted")
        }
        LOG.debug("Discovery service has been stopped")
        runner = null
    }

    /**
     * Start discovery service.
     */
    fun start() {

        // if configured to not start, then simply return
        if (!enabled.get()) {
            LOG.info("Discovery service has been disabled and thus it will not be started")
            return
        }

        // capture driver does not support discovery - nothing to do
        if (support == null) {
            LOG.info("Discovery will not run - driver {} does not support this feature", driver.javaClass.simpleName)
            return
        }

        // return if already running
        if (!running.compareAndSet(false, true)) {
            return
        }

        // start discovery service runner
        runner = Thread(this, "webcam-discovery-service")
        runner!!.uncaughtExceptionHandler = WebcamExceptionHandler.instance
        runner!!.isDaemon = true
        runner!!.start()
    }

    /**
     * Is discovery service running?
     *
     * @return True or false
     */
    fun isRunning(): Boolean {
        return running.get()
    }

    /**
     * Webcam discovery service will be automatically started if it's enabled,
     * otherwise, when set to disabled, it will never start, even when user try
     * to run it.
     *
     * @param enabled the parameter controlling if discovery shall be started
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
    }

    /**
     * Cleanup.
     */
    fun shutdown() {
        stop()
        if (webcams == null) return

        // dispose all webcams
        val wi: Iterator<Webcam> = webcams!!.iterator()
        while (wi.hasNext()) {
            val webcam = wi.next()
            webcam.dispose()
        }
        synchronized(Webcam::class.java) {


            // clear webcams list
            webcams!!.clear()

            // unassign webcams from deallocator
            if (Webcam.isHandleTermSignal) {
                WebcamDeallocator.unstore()
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamDiscoveryService::class.java)
        private fun toWebcams(devices: List<WebcamDevice>): MutableList<Webcam> {
            val webcams: MutableList<Webcam> = ArrayList()
            for (device in devices) {
                webcams.add(Webcam(device))
            }
            return webcams
        }

        /**
         * Get list of devices used by webcams.
         *
         * @return List of webcam devices
         */
        private fun getDevices(webcams: List<Webcam>): List<WebcamDevice> {
            val devices: MutableList<WebcamDevice> = ArrayList()
            for (webcam in webcams) {
                devices.add(webcam.getDevice())
            }
            return devices
        }

        private fun notifyWebcamGone(webcam: Webcam, listeners: Array<WebcamDiscoveryListener>) {
            val event = WebcamDiscoveryEvent(webcam, WebcamDiscoveryEvent.REMOVED)
            for (l in listeners) {
                try {
                    l.webcamGone(event)
                } catch (e: Exception) {
                    LOG.error(String.format("Webcam gone, exception when calling listener %s", l.javaClass), e)
                }
            }
        }

        private fun notifyWebcamFound(webcam: Webcam, listeners: Array<WebcamDiscoveryListener>) {
            val event = WebcamDiscoveryEvent(webcam, WebcamDiscoveryEvent.ADDED)
            for (l in listeners) {
                try {
                    l.webcamFound(event)
                } catch (e: Exception) {
                    LOG.error(String.format("Webcam found, exception when calling listener %s", l.javaClass), e)
                }
            }
        }
    }
}