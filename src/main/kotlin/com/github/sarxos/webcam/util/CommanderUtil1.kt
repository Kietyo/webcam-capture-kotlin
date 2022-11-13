package com.github.sarxos.webcam.util

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * ClassName: CommanderUtil <br></br>
 * Function: utility of process execution <br></br>
 * date: Jan 23, 2019 10:36:09 AM <br></br>
 *
 * @author maoanapex88@163.com alexmao86
 */
object CommanderUtil {
    private val LOGGER = LoggerFactory.getLogger(CommanderUtil::class.java)
    private var seed = 0

    /**
     * DEFAULT_TIMEOUT: default timeout of process execution
     */
    private const val DEFAULT_TIMEOUT = 5000

    /**
     * execute given command, the default timeout is 5 seconds
     *
     * @param cmd
     * @return
     * @since JDK 1.8
     */
    fun execute(cmd: String?): List<String> {
        return execute(cmd, DEFAULT_TIMEOUT)
    }

    /**
     * this will launch one process by given command. the core implementation is,
     * step 1: create one single thread executor service as timeout watcher step 2:
     * submit one watch job to thread pool step 3: create process and consume IO
     * step 4: resource cleanup sequencially or by watchdog asynchronized
     *
     * @param cmd
     * full command line, no OS distinction
     * @param timeout
     * in millseconds
     * @return
     * @since JDK 1.8
     */
    fun execute(cmd: String?, timeout: Int): List<String> {
        val scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor { r -> Thread(r, "Commander-watchdog-" + threadId()) }
        val ret: MutableList<String> = ArrayList()
        val st = StringTokenizer(cmd)
        val cmdarray = arrayOfNulls<String>(st.countTokens())
        var i = 0
        while (st.hasMoreTokens()) {
            cmdarray[i] = st.nextToken()
            i++
        }
        val refer = AtomicReference<Process?>()
        scheduledExecutorService.schedule({
            if (refer.get() != null) {
                refer.get()!!.destroy()
            }
            if (!scheduledExecutorService.isTerminated) {
                scheduledExecutorService.shutdownNow()
            }
        }, timeout.toLong(), TimeUnit.MILLISECONDS)
        try {
            val proc = ProcessBuilder(*cmdarray).directory(File(".")).redirectErrorStream(true)
                .start()
            refer.set(proc)
            val `in` = proc.inputStream
            val reader = BufferedReader(InputStreamReader(`in`))
            var line: String
            while (reader.readLine().also { line = it } != null) {
                if (line.isEmpty()) {
                    continue
                }
                ret.add(line)
            }
            reader.close()
            `in`.close()
            scheduledExecutorService.shutdownNow()
            proc.destroy()
            refer.set(null)
        } catch (e: IOException) {
            LOGGER.warn(e.message)
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug(e.message, e)
            }
        }
        return ret
    }

    private fun threadId(): Int {
        seed++
        return seed
    }
}