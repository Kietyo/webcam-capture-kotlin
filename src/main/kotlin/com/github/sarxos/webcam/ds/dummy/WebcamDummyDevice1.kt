package com.github.sarxos.webcam.ds.dummy

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamException
import com.github.sarxos.webcam.WebcamResolution
import java.awt.*
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Just a dummy device to be used for test purpose.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamDummyDevice(number: Int) : WebcamDevice {
    private val open = AtomicBoolean(false)
    override val resolutions: Array<Dimension> = RESOLUTIONS
    private var backingResolution = resolutions[0]


    override fun getResolution(): Dimension {
        return backingResolution
    }

    override fun setResolution(size: Dimension) {
        backingResolution = size
    }

    override val preallocatedImageBytes: ByteArray
        get() = TODO("Not yet implemented")

    override val name: String = "Dummy Webcam $number"


    var r = (Math.random() * Byte.MAX_VALUE).toInt().toByte()
    var g = (Math.random() * Byte.MAX_VALUE).toInt().toByte()
    var b = (Math.random() * Byte.MAX_VALUE).toInt().toByte()


    private fun drawRect(g2: Graphics2D, w: Int, h: Int) {
        val rx = (w * Math.random() / 1.5).toInt()
        val ry = (h * Math.random() / 1.5).toInt()
        val rw = (w * Math.random() / 1.5).toInt()
        val rh = (w * Math.random() / 1.5).toInt()
        g2.color = Color((Int.MAX_VALUE * Math.random()).toInt())
        g2.fillRect(rx, ry, rw, rh)
    }

    override val image: BufferedImage?
        get() {
            if (!isOpen) {
                throw WebcamException("Webcam is not open")
            }
            try {
                Thread.sleep((1000 / 30).toLong())
            } catch (e: InterruptedException) {
                return null
            }
            val resolution = getResolution()
            val w = resolution.width
            val h = resolution.height
            val s = name
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val gc = ge.defaultScreenDevice.defaultConfiguration
            val bi = gc.createCompatibleImage(w, h)
            val g2 = ge.createGraphics(bi)
            g2.background = Color(Math.abs(r++.toInt()), Math.abs(g++.toInt()), Math.abs(b++.toInt()))
            g2.clearRect(0, 0, w, h)
            drawRect(g2, w, h)
            drawRect(g2, w, h)
            drawRect(g2, w, h)
            drawRect(g2, w, h)
            drawRect(g2, w, h)
            val font = Font("sans-serif", Font.BOLD, 16)
            g2.font = font
            val metrics = g2.getFontMetrics(font)
            val sw = (w - metrics.stringWidth(s)) / 2
            val sh = (h - metrics.height) / 2 + metrics.height / 2
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = Color.BLACK
            g2.drawString(s, sw + 1, sh + 1)
            g2.color = Color.WHITE
            g2.drawString(s, sw, sh)
            g2.dispose()
            bi.flush()
            return bi
        }

    override fun open() {
        if (open.compareAndSet(false, true)) {
            // ...
        }
    }

    override fun close() {
        if (open.compareAndSet(true, false)) {
            // ...
        }
    }

    override fun dispose() {
        close()
    }

    override val isOpen: Boolean
        get() = open.get()

    companion object {
        val RESOLUTIONS = arrayOf(
            WebcamResolution.QQVGA.size,
            WebcamResolution.QVGA.size,
            WebcamResolution.VGA.size
        )
    }
}