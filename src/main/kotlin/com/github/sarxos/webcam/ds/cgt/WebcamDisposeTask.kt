package com.github.sarxos.webcam.ds.cgt

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask

/**
 * Dispose webcam device.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamDisposeTask(driver: WebcamDriver?, device: WebcamDevice?) : WebcamTask(driver!!, device) {
    @Throws(InterruptedException::class)
    fun dispose() {
        process()
    }

    override fun handle() {
        device!!.dispose()
    }
}