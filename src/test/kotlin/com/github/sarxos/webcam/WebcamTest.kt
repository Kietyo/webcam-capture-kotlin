package com.github.sarxos.webcam

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.util.*

class WebcamTest {
    @MockK
    lateinit var driver: WebcamDriver

    @MockK
    lateinit var device: WebcamDevice

    @Test
    fun test_open() {
        MockKAnnotations.init(this)

        every { device.name } returns "HD Mock Device"
        every { device.isOpen } returns false
        every { device.getResolution() } returns Dimension(1024, 768)

        every { device.open() } returns Unit
        every { device.dispose() } returns Unit

        every { driver.devices } returns ArrayList(Arrays.asList(device))
        every { driver.isThreadSafe } returns true

        Webcam.setDriver(driver)
        val webcam = Webcam.getDefault()
        webcam!!.open()
        verify { device.open() }

    }
}