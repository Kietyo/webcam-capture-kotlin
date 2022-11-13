package com.github.sarxos.webcam

import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * Implementation of this interface is responsible for decision whether the difference between two
 * images represents movement or not. Instance may specified as parameter of WebcamMotionDetector
 * constructor, otherwise [WebcamMotionDetectorDefaultAlgorithm] is used.
 */
interface WebcamMotionDetectorAlgorithm {
    /**
     * WebcamMotionDetector calls this method for each image used as parameter of the method
     * [.detect]. Implementation may transform the original
     * image and prepare it for comparison of two images. May return the same instance if no there
     * is no need to transform.
     *
     * @param original image
     * @return modified image
     */
    fun filter(original: BufferedImage): BufferedImage

    /**
     * Detects motion by comparison of the two specified images content.
     * [.filter] method was called for both specified images.
     *
     * @param previous
     * @param current
     * @return If the motion was detected returns true, otherwise returns false
     */
    fun detect(previous: BufferedImage?, current: BufferedImage?): Boolean

    /**
     * Get motion center of gravity. When no motion is detected this value points to the image
     * center. May return null before the first movement check.
     *
     * @return Center of gravity point
     */
    val cog: Point?

    /**
     * Get percentage fraction of image covered by motion. 0 means no motion on image and 100 means
     * full image covered by spontaneous motion.
     *
     * @return Return percentage image fraction covered by motion
     */
    val area: Double
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
    var pointRange: Int
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
    var maxPoints: Int

    /**
     * Returns the currently stored points that have been detected
     *
     * @return The current points
     */
    val points: ArrayList<Point>

    /**
     * Set list of the rectangular zones where motion detection should be ignored.
     *
     * @param bounds the list of rectangles to ignore
     */
    fun setDoNotEngageZones(bounds: List<Rectangle>)
}