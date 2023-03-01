package com.github.sarxos.webcam.util.jh

import com.github.sarxos.webcam.util.jhimport.JHBlurFilter
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class JHBlurFilterTest {
    @Test
    fun test_filterNonPremultiplied() {
        val filter = JHBlurFilter()
        filter.premultiplyAlpha = false
        val bi1 = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val bi2: BufferedImage = filter.filter(bi1, null)
        Assertions
            .assertThat(bi1.width)
            .isEqualTo(bi2.width)
        Assertions
            .assertThat(bi1.height)
            .isEqualTo(bi2.height)
        Assertions
            .assertThat(bi1.type)
            .isEqualTo(bi2.type)
    }

    @Test
    fun test_filterPremultiplied() {
        val filter = JHBlurFilter()
        filter.premultiplyAlpha = true
        val bi1 = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val bi2: BufferedImage = filter.filter(bi1, null)
        Assertions
            .assertThat(bi1.width)
            .isEqualTo(bi2.width)
        Assertions
            .assertThat(bi1.height)
            .isEqualTo(bi2.height)
        Assertions
            .assertThat(bi1.type)
            .isEqualTo(bi2.type)
    }

    @Test
    fun test_setGetPremultiplyAlpha() {
        val filter = JHBlurFilter()
        filter.premultiplyAlpha = true
        Assertions
            .assertThat(filter.premultiplyAlpha)
            .isTrue()
        filter.premultiplyAlpha = false
        Assertions
            .assertThat(filter.premultiplyAlpha)
            .isFalse()
    }
}