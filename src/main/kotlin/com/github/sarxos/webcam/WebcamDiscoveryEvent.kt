package com.github.sarxos.webcam

import java.util.*

/**
 * This event is generated when webcam has been found or lost.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamDiscoveryEvent(
    webcam: Webcam?,
    /**
     * Event type (webcam connected / disconnected).
     */
    val type: Int
) : EventObject(webcam) {
    /**
     * Return the webcam which has been found or removed.
     *
     * @return Webcam instance
     */
    val webcam: Webcam
        get() = getSource() as Webcam

    companion object {
        private const val serialVersionUID = 1L

        /**
         * Event type informing about newly connected webcam.
         */
        const val ADDED = 1

        /**
         * Event type informing about lately disconnected webcam.
         */
        const val REMOVED = 2
    }
}