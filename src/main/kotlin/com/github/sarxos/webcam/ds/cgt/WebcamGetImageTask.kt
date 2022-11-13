package com.github.sarxos.webcam.ds.cgt

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

class WebcamGetImageTask(driver: WebcamDriver?, device: WebcamDevice?) : WebcamTask(driver!!, device) {
    @Volatile
    var image: BufferedImage? = null
        get() {
            try {
                process()
            } catch (e: InterruptedException) {
                LOG.debug("Interrupted exception", e)
                return null
            }
            return field
        }

    override fun handle() {
        val device = device!!
        if (!device.isOpen) {
            return
        }
        image = device.image
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamGetImageTask::class.java)
    }
}