package com.github.sarxos.webcam

import com.github.sarxos.webcam.Webcam.Companion.getDriver
import com.github.sarxos.webcam.ds.test.DummyDriver
import com.github.sarxos.webcam.ds.test.DummyDriver2
import com.github.sarxos.webcam.ds.test.DummyDriver3
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.awt.Dimension
import java.awt.Image
import kotlin.test.*

/**
 * @author bfiryn
 */
class WebcamStaticsTest {
    @BeforeEach
    fun prepare() {
        Webcam.resetDriver()
        println(Thread.currentThread().name + ": Register dummy driver")
        Webcam.registerDriver(DummyDriver::class.java)
    }

    @AfterEach
    fun cleanup() {
        println(Thread.currentThread().name + ": Reset driver")
        for (webcam in Webcam.webcams) {
            webcam.close()
        }
        Webcam.resetDriver()
    }

    @Test
    fun test_getWebcams() {
        println(Thread.currentThread().name + ": test_getWebcams() start")
        val webcams: List<Webcam> = Webcam.webcams
        val devices: List<WebcamDevice> = DummyDriver.instance!!.devices
        assertTrue(webcams.size > 0)
        assertEquals(devices.size.toLong(), webcams.size.toLong())
        println(Thread.currentThread().name + ": test_getWebcams() end")
    }

    @Test
    fun test_getDefault() {
        println(Thread.currentThread().name + ": test_getDefault() start")
        val webcams: List<Webcam> = Webcam.webcams
        val devices: List<WebcamDevice> = DummyDriver.instance!!.devices
        assertNotNull(Webcam.getDefault())
        assertSame(webcams[0], Webcam.getDefault())
        assertSame(devices[0], Webcam.getDefault()!!.getDevice())
        println(Thread.currentThread().name + ": test_getDefault() end")
    }

    @Test
    fun test_open() {
        println(Thread.currentThread().name + ": test_open() start")
        val webcam = Webcam.getDefault()
        webcam!!.open()
        assertTrue(webcam.isOpen())
        webcam.open()
        assertTrue(webcam.isOpen())
        println(Thread.currentThread().name + ": test_open() end")
    }

    @Test
    fun test_close() {
        println(Thread.currentThread().name + ": test_close() start")
        val webcam = Webcam.getDefault()
        webcam!!.open()
        assertIs<DummyDriver>(getDriver())
        assertTrue(webcam.isOpen())
        webcam.close()
        assertFalse(webcam.isOpen())
        webcam.close()
        assertFalse(webcam.isOpen())
        println(Thread.currentThread().name + ": test_close() end")
    }

    @Test
    fun test_getImage() {
        println(Thread.currentThread().name + ": test_getImage() start")
        val webcam = Webcam.getDefault()
        webcam!!.open()
        assertIs<DummyDriver>(getDriver())
        val image: Image? = webcam.image
        assertNotNull(image)
        println(Thread.currentThread().name + ": test_getImage() end")
    }

    @Test
    fun test_getSizes() {
        println(Thread.currentThread().name + ": test_getSizes() start")
        val sizes: Array<Dimension> = Webcam.getDefault()!!.viewSizes
        assertIs<DummyDriver>(getDriver())
        assertNotNull(sizes)
        assertEquals(2, sizes.size.toLong())
        println(Thread.currentThread().name + ": test_getSizes() end")
    }

    @Test
    fun test_setSize() {
        val webcam = Webcam.getDefault()
        val sizes = webcam!!.viewSizes
        webcam.viewSize = sizes[0]
        assertNotNull(webcam.viewSize)
        assertSame(sizes[0], webcam.viewSize)
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_setDriver() {
        Webcam.setDriver(DummyDriver2::class.java)
        val driver2 = getDriver()
//        assertSame<DummyDriver2>(driver2)
        assertIs<DummyDriver2>(driver2)
//        assertSame(DummyDriver2::class.java, driver2.javaClass)
        val driver3: WebcamDriver = DummyDriver3()
        Webcam.setDriver(driver3)
        assertSame(driver3, getDriver())
    }

    @Test
    fun test_registerDriver() {
        Webcam.resetDriver()
        Webcam.registerDriver(DummyDriver::class.java)
        Webcam.webcams
        val driver = getDriver()
        assertIs<DummyDriver>(driver)
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_GetWebcamByName() {
        Webcam.setDriver(DummyDriver())
        for (webcam in Webcam.webcams) {
            assertEquals(webcam.name, Webcam.getWebcamByName(webcam.name)!!.name)
        }
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_GetWebcamByNameWithNotExistingWebcamName() {
        Webcam.setDriver(DummyDriver())
        assertNull(Webcam.getWebcamByName("DatCameraDoesNotExist"))
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_GetWebcamByNameWithNullArgument() {
        Webcam.setDriver(DummyDriver())
        assertThrows<IllegalArgumentException> {
            Webcam.getWebcamByName(null)
        }
    }
}