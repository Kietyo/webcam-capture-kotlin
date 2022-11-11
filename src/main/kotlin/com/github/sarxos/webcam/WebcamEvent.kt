package com.github.sarxos.webcam

import java.awt.image.BufferedImage
import java.util.*

/**
 * Webcam event.
 *
 * @author Bartosz Firyn (SarXos)
 */
class WebcamEvent @JvmOverloads constructor(type: WebcamEventType?, w: Webcam?, image: BufferedImage? = null) :
    EventObject(w) {
    /**
     * Return image acquired by webcam. This method will return not-null object
     * **only** in case new image acquisition event. For all other events, it
     * will simply return null.
     *
     * @return Acquired image
     */
    /**
     * Image acquired from webcam
     */
    var image: BufferedImage? = null
    /**
     * Return event type.
     *
     * @return Event type
     * @see WebcamEventType
     */
    /**
     * Event type.
     */
    var type: WebcamEventType? = null
    /**
     * Webcam event.
     *
     * @param type the event type
     * @param w the webcam object
     * @param image the image acquired from webcam
     */
    /**
     * Webcam event.
     *
     * @param type the event type
     * @param w the webcam object
     */
    init {
        this.type = type
        this.image = image
    }

    override fun getSource(): Webcam {
        return super.getSource() as Webcam
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}