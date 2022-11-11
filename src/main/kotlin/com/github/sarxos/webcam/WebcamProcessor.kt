package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WebcamProcessor private constructor() {
    /**
     * Thread doing supersync processing.
     *
     * @author sarxos
     */
    class ProcessorThread(r: Runnable?) : Thread(r, String.format("atomic-processor-%d", N.incrementAndGet())) {
        companion object {
            private val N = AtomicInteger(0)
        }
    }

    /**
     * Thread factory for processor.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private class ProcessorThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t: Thread = ProcessorThread(r)
            t.uncaughtExceptionHandler = WebcamExceptionHandler.instance
            t.isDaemon = true
            return t
        }
    }

    /**
     * Heart of overall processing system. This class process all native calls wrapped in tasks, by
     * doing this all tasks executions are super-synchronized.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private class AtomicProcessor : Runnable {
        private val inbound = SynchronousQueue<WebcamTask>(true)
        private val outbound = SynchronousQueue<WebcamTask>(true)

        /**
         * Process task.
         *
         * @param task the task to be processed
         * @throws InterruptedException when thread has been interrupted
         */
        @Throws(InterruptedException::class)
        fun process(task: WebcamTask) {
            inbound.put(task)
            val t = outbound.take().throwable
            if (t != null) {
                throw WebcamException("Cannot execute task", t)
            }
        }

        override fun run() {
            while (true) {
                var t: WebcamTask? = null
                try {
                    inbound.take().also { t = it }.handle()
                } catch (e: InterruptedException) {
                    break
                } catch (e: Throwable) {
                    if (t != null) {
                        t!!.throwable = e
                    }
                } finally {
                    if (t != null) {
                        try {
                            outbound.put(t)
                        } catch (e: InterruptedException) {
                            break
                        } catch (e: Exception) {
                            throw RuntimeException("Cannot put task into outbound queue", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Process single webcam task.
     *
     * @param task the task to be processed
     * @throws InterruptedException when thread has been interrupted
     */
    @Throws(InterruptedException::class)
    fun process(task: WebcamTask) {
        if (started.compareAndSet(false, true)) {
            runner = Executors.newSingleThreadExecutor(ProcessorThreadFactory())
            runner!!.execute(processor)
        }
        if (!runner!!.isShutdown) {
            processor.process(task)
        } else {
            throw RejectedExecutionException("Cannot process because processor runner has been already shut down")
        }
    }

    fun shutdown() {
        if (started.compareAndSet(true, false)) {
            LOG.debug("Shutting down webcam processor")
            runner!!.shutdown()
            LOG.debug("Awaiting tasks termination")
            while (runner!!.isTerminated) {
                try {
                    runner!!.awaitTermination(100, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    return
                }
                runner!!.shutdownNow()
            }
            LOG.debug("All tasks has been terminated")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamProcessor::class.java)

        /**
         * Is processor started?
         */
        private val started = AtomicBoolean(false)

        /**
         * Execution service.
         */
        private var runner: ExecutorService? = null

        /**
         * Static processor.
         */
        private val processor = AtomicProcessor()

        /**
         * Singleton instance.
         */
        @get:Synchronized
        val instance = WebcamProcessor()
    }
}