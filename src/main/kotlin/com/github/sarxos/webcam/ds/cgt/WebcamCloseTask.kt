package com.github.sarxos.webcam.ds.cgt

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask
import org.slf4j.LoggerFactory

class WebcamCloseTask(driver: WebcamDriver?, device: WebcamDevice?) : WebcamTask(driver!!, device) {
    @Throws(InterruptedException::class)
    fun close() {
        process()
    }

    override fun handle() {
        val device = device!!
        if (!device.isOpen) {
            return
        }
        LOG.info("Closing {}", device.name)
        device.close()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamCloseTask::class.java)
    }
}