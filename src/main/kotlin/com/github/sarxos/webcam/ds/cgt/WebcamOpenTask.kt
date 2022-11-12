package com.github.sarxos.webcam.ds.cgt

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask
import org.slf4j.LoggerFactory

class WebcamOpenTask(driver: WebcamDriver?, device: WebcamDevice?) : WebcamTask(driver!!, device) {
    @Throws(InterruptedException::class)
    fun open() {
        process()
    }

    override fun handle() {
        val device = device!!
        if (device.isOpen) {
            return
        }
        if (device.getResolution() == null) {
            device.setResolution(device.resolutions[0])
        }
        LOG.info("Opening webcam {}", device.name)
        device.open()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamOpenTask::class.java)
    }
}