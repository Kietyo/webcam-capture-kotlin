package com.github.sarxos.webcam.ds.buildin

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDiscoverySupport
import com.github.sarxos.webcam.WebcamDriver
import com.github.sarxos.webcam.WebcamTask
import com.github.sarxos.webcam.ds.buildin.natives.OpenIMAJGrabber
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * Default build-in webcam driver based on natives from OpenIMAJ framework. It can be widely used on
 * various systems - Mac OS, Linux (x86, x64, ARM), Windows (win32, win64).
 *
 * @author Bartosz Firyn (SarXos)
 */
class WebcamDefaultDriver : WebcamDriver, WebcamDiscoverySupport {
    private class WebcamNewGrabberTask(driver: WebcamDriver) : WebcamTask(driver, null) {
        private val grabber = AtomicReference<OpenIMAJGrabber>()
        fun newGrabber(): OpenIMAJGrabber? {
            try {
                process()
            } catch (e: InterruptedException) {
                LOG.error("Processor has been interrupted")
                return null
            }
            return grabber.get()
        }

        override fun handle() {
            grabber.set(OpenIMAJGrabber())
        }
    }

    private class GetDevicesTask(driver: WebcamDriver) : WebcamTask(driver, null) {
        @Volatile
        private var devices: MutableList<WebcamDevice> = mutableListOf()

        @Volatile
        private var grabber: OpenIMAJGrabber? = null

        /**
         * Return camera devices.
         *
         * @param grabber the native grabber to use for search
         * @return Camera devices.
         */
        fun getDevices(grabber: OpenIMAJGrabber?): List<WebcamDevice>? {
            this.grabber = grabber
            try {
                process()
            } catch (e: InterruptedException) {
                LOG.error("Processor has been interrupted")
                return emptyList()
            }
            return devices
        }

        override fun handle() {
            devices = ArrayList()
            val pointer = grabber!!.videoDevices
            val list = pointer.get()
            for (device in list.asArrayList()) {
                devices.add(WebcamDefaultDevice(device))
            }
        }
    }

    override val devices: List<WebcamDevice>
        get() = run {
            LOG.debug("Searching devices")
            if (grabber == null) {
                val task = WebcamNewGrabberTask(this)
                grabber = task.newGrabber()
                if (grabber == null) {
                    return emptyList()
                }
            }
            val devices = GetDevicesTask(this).getDevices(grabber)!!
            if (LOG.isDebugEnabled) {
                for (device in devices) {
                    LOG.debug("Found device {}", device.name)
                }
            }
            return devices
        }

    override val scanInterval: Long = WebcamDiscoverySupport.DEFAULT_SCAN_INTERVAL

    override val isScanPossible: Boolean = true

    override val isThreadSafe: Boolean
        get() = false

    companion object {
        init {
            if ("true" != System.getProperty("webcam.debug")) {
                System.setProperty("bridj.quiet", "true")
            }
        }

        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamDefaultDriver::class.java)
        private var grabber: OpenIMAJGrabber? = null
    }
}