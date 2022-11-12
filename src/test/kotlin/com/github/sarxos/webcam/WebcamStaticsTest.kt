package com.github.sarxos.webcam

import com.github.sarxos.webcam.Webcam.Companion.getDriver
import com.github.sarxos.webcam.ds.test.DummyDriver
import com.github.sarxos.webcam.ds.test.DummyDriver2
import com.github.sarxos.webcam.ds.test.DummyDriver3
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.awt.Dimension
import java.awt.Image

/**
 * @author bfiryn
 */
class WebcamStaticsTest {
    @Before
    fun prepare() {
        Webcam.resetDriver()
        println(Thread.currentThread().name + ": Register dummy driver")
        Webcam.registerDriver(DummyDriver::class.java)
    }

    @After
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
        val devices: List<WebcamDevice> = DummyDriver.getInstance().devices
        Assert.assertTrue(webcams.size > 0)
        Assert.assertEquals(devices.size.toLong(), webcams.size.toLong())
        println(Thread.currentThread().name + ": test_getWebcams() end")
    }

    @Test
    fun test_getDefault() {
        println(Thread.currentThread().name + ": test_getDefault() start")
        val webcams: List<Webcam> = Webcam.webcams
        val devices: List<WebcamDevice> = DummyDriver.getInstance().devices
        Assert.assertNotNull(Webcam.getDefault())
        Assert.assertSame(webcams[0], Webcam.getDefault())
        Assert.assertSame(devices[0], Webcam.getDefault()!!.getDevice())
        println(Thread.currentThread().name + ": test_getDefault() end")
    }

    @Test
    fun test_open() {
        println(Thread.currentThread().name + ": test_open() start")
        val webcam = Webcam.getDefault()
        webcam!!.open()
        Assert.assertTrue(webcam.isOpen())
        webcam.open()
        Assert.assertTrue(webcam.isOpen())
        println(Thread.currentThread().name + ": test_open() end")
    }

    @Test
    fun test_close() {
        println(Thread.currentThread().name + ": test_close() start")
        val webcam = Webcam.getDefault()
        webcam!!.open()
        Assert.assertSame(DummyDriver::class.java, getDriver().javaClass)
        Assert.assertTrue(webcam.isOpen())
        webcam.close()
        Assert.assertFalse(webcam.isOpen())
        webcam.close()
        Assert.assertFalse(webcam.isOpen())
        println(Thread.currentThread().name + ": test_close() end")
    }

    @Test
    fun test_getImage() {
        println(Thread.currentThread().name + ": test_getImage() start")
        val webcam = Webcam.getDefault()
        webcam!!.open()
        Assert.assertSame(DummyDriver::class.java, getDriver().javaClass)
        val image: Image? = webcam.image
        Assert.assertNotNull(image)
        println(Thread.currentThread().name + ": test_getImage() end")
    }

    @Test
    fun test_getSizes() {
        println(Thread.currentThread().name + ": test_getSizes() start")
        val sizes: Array<Dimension> = Webcam.getDefault()!!.viewSizes
        Assert.assertSame(DummyDriver::class.java, getDriver().javaClass)
        Assert.assertNotNull(sizes)
        Assert.assertEquals(2, sizes.size.toLong())
        println(Thread.currentThread().name + ": test_getSizes() end")
    }

    @Test
    fun test_setSize() {
        val webcam = Webcam.getDefault()
        val sizes = webcam!!.viewSizes
        webcam.viewSize = sizes[0]
        Assert.assertNotNull(webcam.viewSize)
        Assert.assertSame(sizes[0], webcam.viewSize)
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_setDriver() {
        Webcam.setDriver(DummyDriver2::class.java)
        val driver2 = getDriver()
        Assert.assertSame(DummyDriver2::class.java, driver2.javaClass)
        val driver3: WebcamDriver = DummyDriver3()
        Webcam.setDriver(driver3)
        Assert.assertSame(driver3, getDriver())
    }

    @Test
    fun test_registerDriver() {
        Webcam.resetDriver()
        Webcam.registerDriver(DummyDriver::class.java)
        Webcam.webcams
        val driver = getDriver()
        Assert.assertSame(DummyDriver::class.java, driver.javaClass)
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_GetWebcamByName() {
        Webcam.setDriver(DummyDriver())
        for (webcam in Webcam.webcams) {
            Assert.assertEquals(webcam.name, Webcam.getWebcamByName(webcam.name)!!.name)
        }
    }

    @Test
    @Throws(InstantiationException::class)
    fun test_GetWebcamByNameWithNotExistingWebcamName() {
        Webcam.setDriver(DummyDriver())
        Assert.assertNull(Webcam.getWebcamByName("DatCameraDoesNotExist"))
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(InstantiationException::class)
    fun test_GetWebcamByNameWithNullArgument() {
        Webcam.setDriver(DummyDriver())
        Webcam.getWebcamByName(null)
    }
}