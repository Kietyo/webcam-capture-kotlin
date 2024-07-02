package com.github.sarxos.webcam.ds.test

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamException
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

class DummyDevice(override val resolutions: Array<Dimension> = RESOLUTIONS) : WebcamDevice {
    override val preallocatedImageBytes: ByteArray
        get() = TODO("Not yet implemented")
    override val name = DummyDevice::class.java.simpleName + "-" + INSTANCE_NUM.incrementAndGet()
    private var size = resolutions[0]
    override var isOpen = false
        private set

    override fun getResolution(): Dimension {
        return size
    }

    override fun setResolution(size: Dimension) {
        this.size = size
    }

    private var mx = 1
    private var my = 1
    private val r = 10
    private var x = r
    private var y = r
    override val image: BufferedImage
        get() {
            if (!isOpen) {
                throw WebcamException("Not open")
            }
            val bi = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB)
            val g2 = bi.createGraphics()
            g2.color = Color.RED
            g2.fillRect(0, 0, size.width, size.height)
            g2.color = Color.BLACK
            g2.drawString(name, 10, 20)
            g2.color = Color.WHITE
            g2.drawOval(mx.let { x += it; x }, my.let { y += it; y }, r, r)
            g2.dispose()
            bi.flush()
            if (x <= 0 + r || x >= size.width - r) {
                mx = -mx
            }
            if (y <= 0 + r || y >= size.height - r) {
                my = -my
            }
            return bi
        }

    override fun open() {
        isOpen = true
    }

    override fun close() {
        isOpen = false
    }

    override fun dispose() {
        // do nothing
    }

    companion object {
        private val INSTANCE_NUM = AtomicInteger(0)
        val RESOLUTIONS = arrayOf(
            Dimension(300, 200),
            Dimension(400, 300)
        )
    }
}