package com.github.sarxos.webcam

/**
 * Motion listener used to signal motion detection.
 *
 * @author bartosz Firyn (SarXos)
 */
interface WebcamMotionListener {
    /**
     * Will be called after motion is detected.
     *
     * @param wme motion event
     */
    fun motionDetected(wme: WebcamMotionEvent?)
}