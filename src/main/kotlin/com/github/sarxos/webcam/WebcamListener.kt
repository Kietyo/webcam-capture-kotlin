package com.github.sarxos.webcam

/**
 * Webcam listener.
 *
 * @author Bartosz Firyn (SarXos)
 */
interface WebcamListener {
    /**
     * Webcam has been open.
     *
     * @param we a webcam event
     */
    fun webcamOpen(we: WebcamEvent?)

    /**
     * Webcam has been closed
     *
     * @param we a webcam event
     */
    fun webcamClosed(we: WebcamEvent?)

    /**
     * Webcam has been disposed
     *
     * @param we a webcam event
     */
    fun webcamDisposed(we: WebcamEvent?)

    /**
     * Webcam image has been obtained.
     *
     * @param we a webcam event
     */
    fun webcamImageObtained(we: WebcamEvent?)
}