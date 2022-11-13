package com.github.sarxos.webcam

import java.awt.Point
import java.awt.image.BufferedImage
import java.util.*

/**
 * Webcam detected motion event.
 *
 * @author Bartosz Firyn (SarXos)
 */
class WebcamMotionEvent
/**
 * Create detected motion event.
 *
 * @param detector
 * @param previousImage
 * @param currentImage
 * @param strength
 * @param cog center of motion gravity
 */(
    detector: WebcamMotionDetector?,
    /**
     * Returns last image before the motion.
     * Instance is shared among the listeners, so if you need to change the image, create a copy.
     */
    val previousImage: BufferedImage?,
    /**
     * Returns image with the motion detected.
     * Instance is shared among the listeners, so if you need to change the image, create a copy.
     */
    val currentImage: BufferedImage?,
    /**
     * Get percentage fraction of image covered by motion. 0 is no motion on
     * image, and 100 is full image covered by motion.
     *
     * @return Motion area
     */
    val area: Double, val cog: Point?
) : EventObject(detector) {

    /**
     * Create detected motion event.
     *
     * @param detector
     * @param strength
     * @param cog center of motion gravity
     */
    constructor(detector: WebcamMotionDetector?, strength: Double, cog: Point?) : this(
        detector,
        null,
        null,
        strength,
        cog
    ) {
    }

    /**
     * Create detected motion event.
     *
     * @param detector
     * @param strength
     * @param cog center of motion gravity
     * @param points list of all detected points
     */
    constructor(detector: WebcamMotionDetector?, strength: Double, cog: Point?, points: ArrayList<Point>?) : this(
        detector,
        null,
        null,
        strength,
        cog,
        points
    ) {
    }

    /**
     * Create detected motion event.
     *
     * @param detector
     * @param previousImage
     * @param currentImage
     * @param strength
     * @param cog center of motion gravity
     * @param points list of all detected points
     */
    constructor(
        detector: WebcamMotionDetector?,
        previousImage: BufferedImage?,
        currentImage: BufferedImage?,
        strength: Double,
        cog: Point?,
        points: ArrayList<Point>?
    ) : this(detector, previousImage, currentImage, strength, cog) {
        this.points = points
    }

    var points: ArrayList<Point>? = null
        private set
    val webcam: Webcam?
        get() = getSource().webcam

    override fun getSource(): WebcamMotionDetector {
        return super.getSource() as WebcamMotionDetector
    }

    companion object {
        private const val serialVersionUID = -7245768099221999443L
    }
}