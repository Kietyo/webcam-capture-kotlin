package com.github.sarxos.webcam.util.jh

import com.github.sarxos.webcam.util.jhimport.JHFilter
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.Point
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

class JHFilterTest {
    class TestFilter : JHFilter() {
        override fun filter(src: BufferedImage?, dest: BufferedImage?): BufferedImage? {
            return null
        }
    }

    @Test
    fun test_createCompatibleDestImage() {
        val filter = TestFilter()
        val image1 = BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED)
        val image2: BufferedImage = filter.createCompatibleDestImage(image1, null)
        Assertions
            .assertThat(image2.type)
            .isEqualTo(image1.type)
    }

    @Test
    fun test_getBounds2D() {
        val filter = TestFilter()
        val image = BufferedImage(11, 22, BufferedImage.TYPE_BYTE_INDEXED)
        val bounds: Rectangle2D = filter.getBounds2D(image)
        Assertions
            .assertThat(bounds.width)
            .isEqualTo(11.0)
        Assertions
            .assertThat(bounds.height)
            .isEqualTo(22.0)
        Assertions
            .assertThat(bounds.x)
            .isEqualTo(0.0)
        Assertions
            .assertThat(bounds.y)
            .isEqualTo(0.0)
    }

    @Test
    fun test_getPoint2D() {
        val filter = TestFilter()
        val src: Point2D = Point(34, 56)
        val dst: Point2D = Point(67, 78)
        val out: Point2D = filter.getPoint2D(src, dst)
        Assertions
            .assertThat(out)
            .isSameAs(dst)
        Assertions
            .assertThat(out.x)
            .isEqualTo(34.0)
        Assertions
            .assertThat(out.y)
            .isEqualTo(56.0)
    }

    @Test
    fun test_getPoint2DNull() {
        val filter = TestFilter()
        val src: Point2D = Point(34, 56)
        val out: Point2D = filter.getPoint2D(src, null)
        Assertions
            .assertThat(out.x)
            .isEqualTo(34.0)
        Assertions
            .assertThat(out.y)
            .isEqualTo(56.0)
    }

    @Test
    fun test_getRenderingHints() {
        val filter = TestFilter()
        Assertions
            .assertThat(filter.getRenderingHints())
            .isNull()
    }

    @Test
    fun test_setGetRGB() {
        val filter = TestFilter()
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        val p = intArrayOf(0)
        filter.setRGB(image, 0, 0, 1, 1, p)
        val c: IntArray = filter.getRGB(image, 0, 0, 1, 1, p)
        Assertions
            .assertThat(c)
            .hasSize(1)
        Assertions
            .assertThat(c[0])
            .isEqualTo(0)
    }

    @Test
    fun test_setGetRGBIndexed() {
        val filter = TestFilter()
        val image = BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)
        val p = intArrayOf(-16777216)
        filter.setRGB(image, 0, 0, 1, 1, p)
        val c: IntArray = filter.getRGB(image, 0, 0, 1, 1, p)
        Assertions
            .assertThat(c)
            .hasSize(1)
        Assertions
            .assertThat(c[0])
            .isEqualTo(-16777216)
    }
}