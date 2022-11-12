package com.github.sarxos.webcam

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class WebcamResolutionTest {
    @Test
    fun test_getSize() {
        Assertions
            .assertThat(WebcamResolution.VGA.size)
            .isNotNull
        Assertions
            .assertThat(WebcamResolution.VGA.size.getWidth())
            .isEqualTo(640.0)
        Assertions
            .assertThat(WebcamResolution.VGA.size.getHeight())
            .isEqualTo(480.0)
    }

    @Test
    fun test_getPixelCount() {
        Assertions
            .assertThat(WebcamResolution.VGA.pixelsCount)
            .isEqualTo(640 * 480)
    }

    @Test
    fun test_getAspectRatio() {
        Assertions
            .assertThat(WebcamResolution.VGA.aspectRatio)
            .isNotNull
        Assertions
            .assertThat(WebcamResolution.VGA.aspectRatio.getWidth())
            .isEqualTo(4.0)
        Assertions
            .assertThat(WebcamResolution.VGA.aspectRatio.getHeight())
            .isEqualTo(3.0)
    }

    @Test
    fun test_getWidth() {
        Assertions
            .assertThat(WebcamResolution.VGA.width)
            .isEqualTo(640)
    }

    @Test
    fun test_getHeight() {
        Assertions
            .assertThat(WebcamResolution.VGA.height)
            .isEqualTo(480)
    }

    @Test
    fun test_toString() {
        Assertions
            .assertThat(WebcamResolution.VGA.toString())
            .isEqualTo("VGA 640x480 (4:3)")
    }
}