package com.github.sarxos.webcam.ds.cgt

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

class WebcamGetBufferTask(driver: WebcamDriver?, device: WebcamDevice?) : WebcamTask(driver!!, device) {

    @Volatile
    var buffer: ByteBuffer? = null
        get() {
            try {
                process()
            } catch (e: InterruptedException) {
                LOG.debug("Image buffer request interrupted", e)
                return null
            }
            return field
        }

    override fun handle() {
        val device = device!!
        if (!device.isOpen) {
            return
        }
        if (device !is WebcamDevice.BufferAccess) {
            return
        }
        buffer = (device as WebcamDevice.BufferAccess).imageBytes
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamGetBufferTask::class.java)
    }
}