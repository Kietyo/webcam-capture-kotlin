package com.github.sarxos.webcam

import com.github.sarxos.webcam.util.jh.JHBlurFilter
import com.github.sarxos.webcam.util.jh.JHGrayFilter
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * Default motion detector algorithm.
 */
/**
 * @author kevin
 */
class WebcamMotionDetectorDefaultAlgorithm @JvmOverloads constructor(
    pixelThreshold: Int = DEFAULT_PIXEL_THREASHOLD,
    areaThreshold: Double = DEFAULT_AREA_THREASHOLD
) : WebcamMotionDetectorAlgorithm {
    /**
     * Pixel intensity threshold (0 - 255).
     */
    @Volatile
    private var pixelThreshold = DEFAULT_PIXEL_THREASHOLD

    /**
     * Percentage image area fraction threshold (0 - 100).
     */
    @Volatile
    private var areaThreshold = DEFAULT_AREA_THREASHOLD

    /**
     * Maximum pixel change percentage threshold (0 - 100).
     */
    @Volatile
    private var areaThresholdMax = DEFAULT_AREA_THREASHOLD_MAX

    /**
     * Motion strength (0 = no motion, 100 = full image covered by motion).
     */
    override var area = 0.0
        private set

    /**
     * Center of motion gravity.
     */
    override var cog: Point? = null
        private set

    /**
     * Blur filter instance.
     */
    private val blur = JHBlurFilter(6f, 6f, 1)

    /**
     * Gray filter instance.
     */
    private val gray = JHGrayFilter()
    private var doNotEnganeZones = emptyList<Rectangle>()
    override fun filter(original: BufferedImage?): BufferedImage {
        var modified = blur.filter(original, null)
        modified = gray.filter(modified, null)
        return modified
    }

    override fun detect(previous: BufferedImage?, current: BufferedImage?): Boolean {
        points.clear()
        thresholds.clear()
        var p = 0
        var cogX = 0
        var cogY = 0
        val w = current!!.width
        val h = current.height
        var j = 0
        if (previous != null) {
            for (x in 0 until w) {
                for (y in 0 until h) {

                    // ignore point if it's in one of do-not-engage zones, simply skip motion
                    // detection for corresponding pixel
                    if (isInDoNotEngageZone(x, y)) {
                        continue
                    }
                    val cpx = current.getRGB(x, y)
                    val ppx = previous.getRGB(x, y)
                    val pid = combinePixels(cpx, ppx) and 0x000000ff
                    if (pid >= pixelThreshold) {
                        val pp = Point(x, y)
                        var keep = j < maxPoints
                        if (keep) {
                            for (g in points) {
                                if (g.x != pp.x || g.y != pp.y) {
                                    if (pp.distance(g) <= pointRange) {
                                        keep = false
                                        break
                                    }
                                }
                            }
                        }
                        if (keep) {
                            points.add(Point(x, y))
                            j += 1
                        }
                        cogX += x
                        cogY += y
                        p += 1
                        thresholds.add(pid)
                    }
                }
            }
        }
        area = p * 100.0 / (w * h)
        return if (area >= areaThreshold && area <= areaThresholdMax) {
            cog = Point(cogX / p, cogY / p)
            true
        } else {
            cog = Point(w / 2, h / 2)
            false
        }
    }

    /**
     * Return true if point identified by x and y coordinates is in one of the do-not-engage zones.
     * Return false otherwise.
     *
     * @param x the x coordinate of a point
     * @param y the y coordinate of a point
     * @return True if point is in one of do-not-engage zones, false otherwise
     */
    private fun isInDoNotEngageZone(x: Int, y: Int): Boolean {
        for (zone in doNotEnganeZones) {
            if (zone.contains(x, y)) {
                return true
            }
        }
        return false
    }

    /**
     * Set pixel intensity difference threshold above which pixel is classified as "moved". Minimum
     * value is 0 and maximum is 255. Default value is 10. This value is equal for all RGB
     * components difference.
     *
     * @param threshold the pixel intensity difference threshold
     * @see .DEFAULT_PIXEL_THREASHOLD
     */
    fun setPixelThreshold(threshold: Int) {
        require(threshold >= 0) { "Pixel intensity threshold cannot be negative!" }
        require(threshold <= 255) { "Pixel intensity threshold cannot be higher than 255!" }
        pixelThreshold = threshold
    }

    /**
     * Set percentage fraction of detected motion area threshold above which it is classified as
     * "moved". Minimum value for this is 0 and maximum is 100, which corresponds to full image
     * covered by spontaneous motion.
     *
     * @param threshold the percentage fraction of image area
     * @see .DEFAULT_AREA_THREASHOLD
     */
    fun setAreaThreshold(threshold: Double) {
        require(threshold >= 0) { "Area fraction threshold cannot be negative!" }
        require(threshold <= 100) { "Area fraction threshold cannot be higher than 100!" }
        areaThreshold = threshold
    }

    /**
     * Set max percentage fraction of detected motion area threshold, below which it is classified
     * as "moved". The max is optionally used to help filter false positives caused by sudden
     * exposure changes. Minimum value for this is 0 and maximum is 100, which corresponds to full
     * image covered by spontaneous motion.
     *
     * @param threshold the percentage fraction of image area
     * @see .DEFAULT_AREA_THREASHOLD_MAX
     */
    fun setMaxAreaThreshold(threshold: Double) {
        require(threshold >= 0) { "Area fraction threshold cannot be negative!" }
        require(threshold <= 100) { "Area fraction threshold cannot be higher than 100!" }
        areaThresholdMax = threshold
    }
    /**
     * Returns the currently stored points that have been detected
     *
     * @return The current points
     */
    /**
     * ArrayList to store the points for a detected motion
     */
    override var points = ArrayList<Point>()

    /**
     * Array list to store thresholds for debugging
     */
    var thresholds = ArrayList<Int>()
    /**
     * Get the current minimum range between each point
     *
     * @return The current range
     */
    /**
     * Set the minimum range between each point detected
     *
     * @param i the range to set
     */
    /**
     * The current minimum range between points
     */
    override var pointRange = DEFAULT_RANGE
    /**
     * Get the current max amount of points that can be detected at one time
     *
     * @return
     */
    /**
     * Set the max amount of points that can be detected at one time
     *
     * @param i The amount of points that can be detected
     */
    /**
     * The current max amount of points
     */
    override var maxPoints = DEFAULT_MAX_POINTS
    /**
     * Creates default motion detector algorithm.
     *
     * @param pixelThreshold intensity threshold (0 - 255)
     * @param areaThreshold percentage threshold of image covered by motion
     */
    /**
     * Creates default motion detector algorithm with default pixel and area thresholds.
     *
     * @see .DEFAULT_PIXEL_THREASHOLD
     *
     * @see .DEFAULT_AREA_THREASHOLD
     */
    init {
        setPixelThreshold(pixelThreshold)
        setAreaThreshold(areaThreshold)
    }

    override fun setDoNotEngageZones(bounds: List<Rectangle>) {
        doNotEnganeZones = bounds
    }

    companion object {
        /**
         * Default pixel difference intensity threshold (set to 25).
         */
        const val DEFAULT_PIXEL_THREASHOLD = 25

        /**
         * Default percentage image area fraction threshold (set to 0.2%).
         */
        const val DEFAULT_AREA_THREASHOLD = 0.2

        /**
         * Default max percentage image area fraction threshold (set to 100%).
         */
        const val DEFAULT_AREA_THREASHOLD_MAX = 100.0
        private fun combinePixels(rgb1: Int, rgb2: Int): Int {

            // first ARGB
            var a1 = rgb1 shr 24 and 0xff
            var r1 = rgb1 shr 16 and 0xff
            var g1 = rgb1 shr 8 and 0xff
            var b1 = rgb1 and 0xff

            // second ARGB
            val a2 = rgb2 shr 24 and 0xff
            val r2 = rgb2 shr 16 and 0xff
            val g2 = rgb2 shr 8 and 0xff
            val b2 = rgb2 and 0xff
            r1 = clamp(Math.abs(r1 - r2))
            g1 = clamp(Math.abs(g1 - g2))
            b1 = clamp(Math.abs(b1 - b2))

            // in case if alpha is enabled (translucent image)
            if (a1 != 0xff) {
                a1 = a1 * 0xff / 255
                val a3 = (255 - a1) * a2 / 255
                r1 = clamp((r1 * a1 + r2 * a3) / 255)
                g1 = clamp((g1 * a1 + g2 * a3) / 255)
                b1 = clamp((b1 * a1 + b2 * a3) / 255)
                a1 = clamp(a1 + a3)
            }
            return a1 shl 24 or (r1 shl 16) or (g1 shl 8) or b1
        }

        /**
         * Clamp a value to the range 0..255
         */
        private fun clamp(c: Int): Int {
            if (c < 0) {
                return 0
            }
            return if (c > 255) {
                255
            } else c
        }

        /**
         * The default minimum range between each point where motion has been detected
         */
        const val DEFAULT_RANGE = 50

        /**
         * The default for the max amount of points that can be detected at one time
         */
        const val DEFAULT_MAX_POINTS = 100
    }
}