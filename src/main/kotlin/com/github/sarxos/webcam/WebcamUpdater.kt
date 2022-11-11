package com.github.sarxos.webcam

import com.github.sarxos.webcam.Webcam.Companion.getDriver
import com.github.sarxos.webcam.WebcamDevice.FPSSource
import com.github.sarxos.webcam.WebcamUpdater
import com.github.sarxos.webcam.ds.cgt.WebcamGetImageTask
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * The goal of webcam updater class is to update image in parallel, so all calls to fetch image
 * invoked on webcam instance will be non-blocking (will return immediately).
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamUpdater(webcam: Webcam, delayCalculator: DelayCalculator?) : Runnable {
    /**
     * Implementation of this interface is responsible for calculating the delay between 2 image
     * fetching, when the non-blocking (asynchronous) access to the webcam is enabled.
     */
    interface DelayCalculator {
        /**
         * Calculates delay before the next image will be fetched from the webcam. Must return
         * number greater or equal 0.
         *
         * @param snapshotDuration - duration of taking the last image
         * @param deviceFps - current FPS obtained from the device, or -1 if the driver doesn't
         * support it
         * @return interval (in millis)
         */
        fun calculateDelay(snapshotDuration: Long, deviceFps: Double): Long
    }

    /**
     * Default impl of DelayCalculator, based on TARGET_FPS. Returns 0 delay for snapshotDuration
     * &gt; 20 millis.
     */
    class DefaultDelayCalculator : DelayCalculator {
        override fun calculateDelay(snapshotDuration: Long, deviceFps: Double): Long {
            // Calculate delay required to achieve target FPS.
            // In some cases it can be less than 0
            // because camera is not able to serve images as fast as
            // we would like to. In such case just run with no delay,
            // so maximum FPS will be the one supported
            // by camera device in the moment.
            return Math.max(1000 / TARGET_FPS - snapshotDuration, 0)
        }
    }

    /**
     * Thread factory for executors used within updater class.
     *
     * @author Bartosz Firyn (sarxos)
     */
    private class UpdaterThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(r, String.format("webcam-updater-thread-%d", number.incrementAndGet()))
            t.uncaughtExceptionHandler = WebcamExceptionHandler.getInstance()
            t.isDaemon = true
            return t
        }

        companion object {
            private val number = AtomicInteger(0)
        }
    }

    /**
     * Executor service.
     */
    private var executor: ScheduledExecutorService? = null

    /**
     * Cached image.
     */
    private val image = AtomicReference<BufferedImage?>()

    /**
     * Webcam to which this updater is attached.
     */
    private val webcam: Webcam
    /**
     * Return current FPS number. It is calculated in real-time on the base of how often camera
     * serve new image.
     *
     * @return FPS number
     */
    /**
     * Current FPS rate.
     */
    @Volatile
    var fPS = 0.0
        private set

    /**
     * Is updater running.
     */
    private val running = AtomicBoolean(false)

    @Volatile
    var isImageNew = false
        private set

    /**
     * DelayCalculator implementation.
     */
    private val delayCalculator: DelayCalculator

    /**
     * Construct new webcam updater using DefaultDelayCalculator.
     *
     * @param webcam the webcam to which updater shall be attached
     */
    protected constructor(webcam: Webcam) : this(webcam, DefaultDelayCalculator()) {}

    /**
     * Construct new webcam updater.
     *
     * @param webcam the webcam to which updater shall be attached
     * @param delayCalculator implementation
     */
    init {
        this.webcam = webcam
        if (delayCalculator == null) {
            this.delayCalculator = DefaultDelayCalculator()
        } else {
            this.delayCalculator = delayCalculator
        }
    }

    /**
     * Start updater.
     */
    fun start() {
        if (running.compareAndSet(false, true)) {
            image.set(WebcamGetImageTask(getDriver(), webcam!!.getDevice()).image)
            executor = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY)
            executor!!.execute(this)
            LOG.debug("Webcam updater has been started")
        } else {
            LOG.debug("Webcam updater is already started")
        }
    }

    /**
     * Stop updater.
     */
    fun stop() {
        if (running.compareAndSet(true, false)) {
            executor!!.shutdown()
            while (!executor!!.isTerminated) {
                try {
                    executor!!.awaitTermination(100, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    return
                }
            }
            LOG.debug("Webcam updater has been stopped")
        } else {
            LOG.debug("Webcam updater is already stopped")
        }
    }

    override fun run() {
        if (!running.get()) {
            return
        }
        try {
            tick()
        } catch (t: Throwable) {
            WebcamExceptionHandler.handle(t)
        }
    }

    private fun tick() {
        if (!webcam!!.isOpen()) {
            return
        }

        // Calculate time required to fetch 1 picture.
        val driver = getDriver()
        val device = webcam.getDevice()
        assert(driver != null)
        assert(device != null)
        var imageOk = false
        val t1 = System.currentTimeMillis()
        try {
            image.set(webcam.transform(WebcamGetImageTask(driver, device).image))
            isImageNew = true
            imageOk = true
        } catch (e: WebcamException) {
            WebcamExceptionHandler.handle(e)
        }
        val t2 = System.currentTimeMillis()
        var deviceFps = -1.0
        if (device is FPSSource) {
            deviceFps = (device as FPSSource).fps
        }
        val duration = t2 - t1
        val delay = delayCalculator!!.calculateDelay(duration, deviceFps)
        val delta = duration + 1 // +1 to avoid division by zero
        if (deviceFps >= 0) {
            fPS = deviceFps
        } else {
            fPS = (4 * fPS + 1000 / delta) / 5
        }

        // reschedule task
        if (webcam.isOpen()) {
            try {
                executor!!.schedule(this, delay, TimeUnit.MILLISECONDS)
            } catch (e: RejectedExecutionException) {
                LOG.trace("Webcam update has been rejected", e)
            }
        }

        // notify webcam listeners about the new image available
        if (imageOk) {
            webcam.notifyWebcamImageAcquired(image.get())
        }
    }

    /**
     * Return currently available image. This method will return immediately while it was been
     * called after camera has been open. In case when there are parallel threads running and there
     * is a possibility to call this method in the opening time, or before camera has been open at
     * all, this method will block until webcam return first image. Maximum blocking time will be 10
     * seconds, after this time method will return null.
     *
     * @return Image stored in cache
     */
    fun getImage(): BufferedImage? {
        var i = 0
        while (image.get() == null) {

            // Just in case if another thread starts calling this method before
            // updater has been properly started. This will loop while image is
            // not available.
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            // Return null if more than 10 seconds passed (timeout).
            if (i++ > 100) {
                LOG.error("Image has not been found for more than 10 seconds")
                return null
            }
        }
        isImageNew = false
        return image.get()
    }

    companion object {
        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamUpdater::class.java)

        /**
         * Target FPS.
         */
        private const val TARGET_FPS = 50
        private val THREAD_FACTORY = UpdaterThreadFactory()
    }
}