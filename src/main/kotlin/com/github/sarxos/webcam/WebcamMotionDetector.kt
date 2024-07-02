package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamExceptionHandler.Companion.handle
import com.github.sarxos.webcam.WebcamMotionDetector
import org.slf4j.LoggerFactory
import java.awt.Point
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Webcam motion detector.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamMotionDetector(
    /**
     * Webcam to be used to detect motion.
     */
    val webcam: Webcam?, algorithm: WebcamMotionDetectorAlgorithm, interval: Int) {
    /**
     * Create new threads for detector internals.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private class DetectorThreadFactory : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread {
            val t = Thread(runnable, String.format("motion-detector-%d", NT.incrementAndGet()))
            t.uncaughtExceptionHandler = WebcamExceptionHandler.instance
            t.isDaemon = true
            return t
        }
    }

    /**
     * Run motion detector.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private inner class Runner : Runnable {
        override fun run() {
            running.set(true)
            while (running.get() && webcam!!.isOpen()) {
                try {
                    detect()
                    Thread.sleep(interval.toLong())
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    handle(e)
                }
            }
            running.set(false)
        }
    }

    /**
     * Change motion to false after specified number of seconds.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private inner class Inverter : Runnable {
        override fun run() {
            var delay: Int
            while (running.get()) {
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    break
                }
                delay = if (inertia != -1) inertia else 2 * interval
                if (lastMotionTimestamp + delay < System.currentTimeMillis()) {
                    motion = false
                }
            }
        }
    }

    /**
     * Executor.
     */
    private val executor = Executors.newFixedThreadPool(2, THREAD_FACTORY)

    /**
     * Motion listeners.
     */
    private val listeners: MutableList<WebcamMotionListener> = ArrayList()

    /**
     * Is detector running?
     */
    private val running = AtomicBoolean(false)

    /**
     * Is motion?
     */
    @Volatile
    private var motion = false

    /**
     * Previously captured image.
     */
    private var previousOriginal: BufferedImage? = null

    /**
     * Previously captured image with blur and gray filters applied.
     */
    private var previousFiltered: BufferedImage? = null
    /**
     * Get attached webcam object.
     *
     * @return Attached webcam
     */


    /**
     * Motion check interval (1000 ms by default).
     */
    @Volatile
    private var interval = DEFAULT_INTERVAL

    /**
     * How long motion is valid (in milliseconds). Default value is 2 seconds.
     */
    @Volatile
    private var inertia = -1

    /**
     * Timestamp when motion has been observed last time.
     */
    @Volatile
    private var lastMotionTimestamp: Long = 0
    /**
     * @return the detectorAlgorithm
     */
    /**
     * Implementation of motion detection algorithm.
     */
    val detectorAlgorithm: WebcamMotionDetectorAlgorithm

    /**
     * Create motion detector. Will open webcam if it is closed.
     *
     * @param webcam web camera instance
     * @param motion detector algorithm implementation
     * @param interval the check interval (in milliseconds)
     */
    init {
        detectorAlgorithm = algorithm
        setInterval(interval)
    }
    /**
     * Create motion detector. Will open webcam if it is closed. Uses
     * WebcamMotionDetectorDefaultAlgorithm for motion detection.
     *
     * @param webcam web camera instance
     * @param pixelThreshold intensity threshold (0 - 255)
     * @param areaThreshold percentage threshold of image covered by motion
     * @param interval the check interval
     */
    /**
     * Create motion detector with default parameter inertia = 0. Uses
     * WebcamMotionDetectorDefaultAlgorithm for motion detection.
     *
     * @param webcam web camera instance
     * @param pixelThreshold intensity threshold (0 - 255)
     * @param areaThreshold percentage threshold of image covered by motion (0 - 100)
     */
    /**
     * Create motion detector with default parameter inertia = 0. Uses
     * WebcamMotionDetectorDefaultAlgorithm for motion detection.
     *
     * @param webcam web camera instance
     * @param pixelThreshold intensity threshold (0 - 255)
     */
    /**
     * Create motion detector with default parameters - threshold = 25, inertia = 0.
     *
     * @param webcam web camera instance
     */
    @JvmOverloads
    constructor(
        webcam: Webcam?,
        pixelThreshold: Int = WebcamMotionDetectorDefaultAlgorithm.Companion.DEFAULT_PIXEL_THREASHOLD,
        areaThreshold: Double = WebcamMotionDetectorDefaultAlgorithm.Companion.DEFAULT_AREA_THREASHOLD,
        interval: Int = DEFAULT_INTERVAL
    ) : this(webcam, WebcamMotionDetectorDefaultAlgorithm(pixelThreshold, areaThreshold), interval) {
    }

    fun start() {
        if (running.compareAndSet(false, true)) {
            webcam!!.open()
            executor.submit(Runner())
            executor.submit(Inverter())
        }
    }

    fun stop() {
        if (running.compareAndSet(true, false)) {
            webcam!!.close()
            executor.shutdownNow()
        }
    }

    protected fun detect() {
        if (!webcam!!.isOpen()) {
            motion = false
            return
        }
        val currentOriginal = webcam.image
        if (currentOriginal == null) {
            motion = false
            return
        }
        val currentFiltered = detectorAlgorithm.filter(currentOriginal)
        val motionDetected = detectorAlgorithm.detect(previousFiltered, currentFiltered)
        if (motionDetected) {
            motion = true
            lastMotionTimestamp = System.currentTimeMillis()
            notifyMotionListeners(currentOriginal)
        }
        previousOriginal = currentOriginal
        previousFiltered = currentFiltered
    }

    /**
     * Will notify all attached motion listeners.
     *
     * @param image with the motion detected
     */
    private fun notifyMotionListeners(currentOriginal: BufferedImage) {
        val wme = WebcamMotionEvent(
            this,
            previousOriginal,
            currentOriginal,
            detectorAlgorithm.area,
            detectorAlgorithm.cog,
            detectorAlgorithm.points
        )
        for (l in listeners) {
            try {
                l.motionDetected(wme)
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    /**
     * Add motion listener.
     *
     * @param l listener to add
     * @return true if listeners list has been changed, false otherwise
     */
    fun addMotionListener(l: WebcamMotionListener): Boolean {
        return listeners.add(l)
    }

    /**
     * @return All motion listeners as array
     */
    val motionListeners: Array<WebcamMotionListener>
        get() = listeners.toTypedArray()

    /**
     * Removes motion listener.
     *
     * @param l motion listener to remove
     * @return true if listener was available on the list, false otherwise
     */
    fun removeMotionListener(l: WebcamMotionListener): Boolean {
        return listeners.remove(l)
    }

    /**
     * @return Motion check interval in milliseconds
     */
    fun getInterval(): Int {
        return interval
    }

    /**
     * Motion check interval in milliseconds. After motion is detected, it's valid for time which is
     * equal to value of 2 * interval.
     *
     * @param interval the new motion check interval (ms)
     * @see .DEFAULT_INTERVAL
     */
    fun setInterval(interval: Int) {
        require(interval >= 100) { "Motion check interval cannot be less than 100 ms" }
        this.interval = interval
    }

    /**
     * Sets pixelThreshold to the underlying detector algorithm, but only if the algorithm is (or
     * extends) WebcamMotionDetectorDefaultAlgorithm
     *
     * @see WebcamMotionDetectorDefaultAlgorithm.setPixelThreshold
     * @param threshold the pixel intensity difference threshold
     */
    fun setPixelThreshold(threshold: Int) {
        (detectorAlgorithm as WebcamMotionDetectorDefaultAlgorithm).setPixelThreshold(threshold)
    }

    /**
     * Sets areaThreshold to the underlying detector algorithm, but only if the algorithm is (or
     * extends) WebcamMotionDetectorDefaultAlgorithm
     *
     * @see WebcamMotionDetectorDefaultAlgorithm.setAreaThreshold
     * @param threshold the percentage fraction of image area
     */
    fun setAreaThreshold(threshold: Double) {
        (detectorAlgorithm as WebcamMotionDetectorDefaultAlgorithm).setAreaThreshold(threshold)
    }

    /**
     * Set motion inertia (time when motion is valid). If no value specified this is set to 2 *
     * interval. To reset to default value, [.clearInertia] method must be used.
     *
     * @param inertia the motion inertia time in milliseconds
     * @see .clearInertia
     */
    fun setInertia(inertia: Int) {
        require(inertia >= 0) { "Inertia time must not be negative!" }
        this.inertia = inertia
    }

    /**
     * Reset inertia time to value calculated automatically on the base of interval. This value will
     * be set to 2 * interval.
     */
    fun clearInertia() {
        inertia = -1
    }

    fun isMotion(): Boolean {
        if (!running.get()) {
            LOG.warn("Motion cannot be detected when detector is not running!")
        }
        return motion
    }

    /**
     * Get percentage fraction of image covered by motion. 0 means no motion on image and 100 means
     * full image covered by spontaneous motion.
     *
     * @return Return percentage image fraction covered by motion
     */
    val motionArea: Double
        get() = detectorAlgorithm.area// detectorAlgorithm hasn't been called so far - get image center

    /**
     * Get motion center of gravity. When no motion is detected this value points to the image
     * center.
     *
     * @return Center of gravity point
     */
    val motionCog: Point
        get() {
            var cog = detectorAlgorithm.cog
            if (cog == null) {
                // detectorAlgorithm hasn't been called so far - get image center
                val w = webcam!!.viewSize.width
                val h = webcam.viewSize.height
                cog = Point(w / 2, h / 2)
            }
            return cog
        }
    var maxMotionPoints: Int
        get() = detectorAlgorithm.maxPoints
        set(i) {
            detectorAlgorithm.maxPoints = i
        }
    var pointRange: Int
        get() = detectorAlgorithm.pointRange
        set(i) {
            detectorAlgorithm.pointRange = i
        }

    companion object {
        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamMotionDetector::class.java)

        /**
         * Thread number in pool.
         */
        private val NT = AtomicInteger(0)

        /**
         * Thread factory.
         */
        private val THREAD_FACTORY: ThreadFactory = DetectorThreadFactory()

        /**
         * Default check interval, in milliseconds, set to 500 ms.
         */
        const val DEFAULT_INTERVAL = 500
    }
}