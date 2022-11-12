package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamPanel.DrawMode
import com.github.sarxos.webcam.ds.test.DummyDriver
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.Dimension

class WebcamPanelTest {
    private val WIDTH = 256
    private val HEIGHT = 345
    @Test
    @Throws(InterruptedException::class)
    fun test_size() {
        Webcam.setDriver(DummyDriver())
        val w: Webcam = Webcam.getDefault()!!
        val p = WebcamPanel(w)
        w.open()
        p.repaint()
        val bi = w.image
        val d = p.preferredSize
        Assertions
            .assertThat(d.getWidth())
            .isEqualTo(bi!!.width.toDouble())
        Assertions
            .assertThat(d.getHeight())
            .isEqualTo(bi.height.toDouble())
        p.stop()
        w.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun test_sizeSpecified() {
        Webcam.setDriver(DummyDriver())
        val w: Webcam = Webcam.getDefault()!!
        val p = WebcamPanel(w, Dimension(WIDTH, HEIGHT), false)
        w.open()
        p.repaint()
        val d = p.preferredSize
        Assertions
            .assertThat(d.getWidth())
            .isEqualTo(WIDTH.toDouble())
        Assertions
            .assertThat(d.getHeight())
            .isEqualTo(HEIGHT.toDouble())
        p.stop()
        w.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun test_modeFill() {
        Webcam.setDriver(DummyDriver())
        val w: Webcam = Webcam.getDefault()!!
        w.open()
        val p = WebcamPanel(w, Dimension(WIDTH, HEIGHT), false)
        p.drawMode = DrawMode.FILL
        p.start()
        Assertions
            .assertThat(p.drawMode)
            .isEqualTo(DrawMode.FILL)
        ScreenImage.createImage(p)
        Thread.sleep(100)
        p.stop()
        w.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun test_modeFit() {
        Webcam.setDriver(DummyDriver())
        val w: Webcam = Webcam.getDefault()!!
        w.open()
        val p = WebcamPanel(w, Dimension(WIDTH, HEIGHT), false)
        p.drawMode = DrawMode.FIT
        p.start()
        Assertions
            .assertThat(p.drawMode)
            .isEqualTo(DrawMode.FIT)
        ScreenImage.createImage(p)
        Thread.sleep(100)
        p.stop()
        w.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun test_modeNone() {
        Webcam.setDriver(DummyDriver())
        val w: Webcam = Webcam.getDefault()!!
        w.open()
        val p = WebcamPanel(w, Dimension(WIDTH, HEIGHT), false)
        p.drawMode = DrawMode.NONE
        p.start()
        Assertions
            .assertThat(p.drawMode)
            .isEqualTo(DrawMode.NONE)
        ScreenImage.createImage(p)
        Thread.sleep(100)
        p.stop()
        w.close()
    }
}