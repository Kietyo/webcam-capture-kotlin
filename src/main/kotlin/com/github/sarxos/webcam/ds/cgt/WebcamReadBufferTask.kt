package com.github.sarxos.webcam.ds.cgt

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask
import java.nio.ByteBuffer

class WebcamReadBufferTask(driver: WebcamDriver?, device: WebcamDevice?, target: ByteBuffer?) : WebcamTask(
    driver!!, device
) {
    private val target: ByteBuffer? = target

    fun readBuffer(): ByteBuffer? {
        try {
            process()
        } catch (e: InterruptedException) {
            return null
        }
        return target
    }

    override fun handle() {
        val device = device!!
        if (!device.isOpen) {
            return
        }
        if (device !is WebcamDevice.BufferAccess) {
            return
        }
        (device as WebcamDevice.BufferAccess).getImageBytes(target!!)
    }
}