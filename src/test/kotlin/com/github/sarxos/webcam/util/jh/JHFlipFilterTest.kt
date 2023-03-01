package com.github.sarxos.webcam.util.jh

import com.github.sarxos.webcam.util.ImageUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.IOException

class JHFlipFilterTest {
    val original: BufferedImage = ImageUtils.readFromResource("tv-test-pattern.png")
    @Test
    @Throws(IOException::class)
    fun test_FLIP_90CW() {
        val filter = JHFlipFilter(JHFlipFilter.FLIP_90CW)
        val rotated = filter.filter(original, null)
        Assertions
            .assertThat(original.width)
            .isEqualTo(rotated.height)
        Assertions
            .assertThat(original.height)
            .isEqualTo(rotated.width)
        Assertions
            .assertThat(filter.toString())
            .isEqualTo("Rotate 90")
    }

    @Test
    @Throws(IOException::class)
    fun test_FLIP_90CCW() {
        val filter = JHFlipFilter(JHFlipFilter.FLIP_90CCW)
        val rotated = filter.filter(original, null)
        Assertions
            .assertThat(original.width)
            .isEqualTo(rotated.height)
        Assertions
            .assertThat(original.height)
            .isEqualTo(rotated.width)
        Assertions
            .assertThat(filter.toString())
            .isEqualTo("Rotate -90")
    }

    @Test
    @Throws(IOException::class)
    fun test_FLIP_180() {
        val filter = JHFlipFilter(JHFlipFilter.FLIP_180)
        val rotated = filter.filter(original, null)
        Assertions
            .assertThat(original.width)
            .isEqualTo(rotated.width)
        Assertions
            .assertThat(original.height)
            .isEqualTo(rotated.height)
        Assertions
            .assertThat(filter.toString())
            .isEqualTo("Rotate 180")
    }

    @Test
    @Throws(IOException::class)
    fun test_default() {
        val filter = JHFlipFilter()
        Assertions
            .assertThat(filter.operation)
            .isEqualTo(JHFlipFilter.FLIP_90CW)
    }

    @Test
    @Throws(IOException::class)
    fun test_setOperation() {
        val filter = JHFlipFilter()
        filter.operation = 888
        Assertions
            .assertThat(filter.operation)
            .isEqualTo(888)
        Assertions
            .assertThat(filter.toString())
            .isEqualTo("Flip")
    }
}