package com.github.sarxos.webcam.ds.buildin

import com.github.sarxos.webcam.*
import com.github.sarxos.webcam.WebcamDevice.FPSSource
import com.github.sarxos.webcam.ds.buildin.natives.Device
import com.github.sarxos.webcam.ds.buildin.natives.OpenIMAJGrabber
import org.bridj.Pointer
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class WebcamDefaultDevice(device: Device) : WebcamDevice, WebcamDevice.BufferAccess, Runnable,
    FPSSource {
    private inner class NextFrameTask(device: WebcamDevice?) : WebcamTask(device!!) {
        private val result = AtomicInteger(0)
        fun nextFrame(): Int {
            try {
                process()
            } catch (e: InterruptedException) {
                LOG.debug("Image buffer request interrupted", e)
            }
            return result.get()
        }

        override fun handle() {
            val device = device as WebcamDefaultDevice
            if (!device.isOpen) {
                return
            }
            result.set(grabber!!.nextFrame())
            fresh.set(true)
        }
    }

    /**
     * Maximum image acquisition time (in milliseconds).
     */
    private var timeout = 5000
    private var grabber: OpenIMAJGrabber? = null
    private var size: Dimension? = null
    private lateinit var smodel: ComponentSampleModel
    private val cmodel: ColorModel = ComponentColorModel(COLOR_SPACE, BITS, false, false, Transparency.OPAQUE, DATA_TYPE)
    private var failOnSizeMismatch = false
    private val disposed = AtomicBoolean(false)
    private val open = AtomicBoolean(false)

    /**
     * Is the last image fresh one.
     */
    private val fresh = AtomicBoolean(false)
    private var refresher: Thread? = null
    val deviceRef: Device = device
    val deviceName: String = device.nameStr
    val deviceId: String = device.identifierStr
    private val fullname: String = String.format("%s %s", deviceName, deviceId)
    private var t1: Long = -1
    private var t2: Long = -1

    /**
     * Current FPS.
     */
    @Volatile
    override var fps = 0.0

    override val name: String = fullname

    override val resolutions: Array<Dimension> = DIMENSIONS

    override fun getResolution(): Dimension {
        if (size == null) {
            size = resolutions[0]
        }
        return size!!
    }

    override fun setResolution(size: Dimension) {
        check(!isOpen) { "Cannot change resolution when webcam is open, please close it first" }
        this.size = size
    }

    override val imageBytes: ByteBuffer?
        get() {
            if (disposed.get()) {
                LOG.debug("Webcam is disposed, image will be null")
                return null
            }
            if (!open.get()) {
                LOG.debug("Webcam is closed, image will be null")
                return null
            }

            // if image is not fresh, update it
            if (fresh.compareAndSet(false, true)) {
                updateFrameBuffer()
            }

            // get image buffer
            LOG.trace("Webcam grabber get image pointer")
            val image = grabber!!.image
            fresh.set(false)
            if (image == null) {
                LOG.warn("Null array pointer found instead of image")
                return null
            }
            val length = size!!.width * size!!.height * 3
            LOG.trace("Webcam device get buffer, read {} bytes", length)
            return image.getByteBuffer(length.toLong())
        }

    override fun getImageBytes(target: ByteBuffer) {
        if (disposed.get()) {
            LOG.debug("Webcam is disposed, image will be null")
            return
        }
        if (!open.get()) {
            LOG.debug("Webcam is closed, image will be null")
            return
        }
        val minSize = size!!.width * size!!.height * 3
        val curSize = target.remaining()
        require(minSize <= curSize) {
            String.format(
                "Not enough remaining space in target buffer (%d necessary vs %d remaining)",
                minSize,
                curSize
            )
        }

        // if image is not fresh, update it
        if (fresh.compareAndSet(false, true)) {
            updateFrameBuffer()
        }

        // get image buffer
        LOG.trace("Webcam grabber get image pointer")
        var image = grabber!!.image
        fresh.set(false)
        if (image == null) {
            LOG.warn("Null array pointer found instead of image")
            return
        }
        LOG.trace("Webcam device read buffer {} bytes", minSize)
        image = image.validBytes(minSize.toLong())
        image.getBytes(target)
    }

    override val image: BufferedImage?
        get() {
            val buffer = imageBytes
            if (buffer == null) {
                LOG.error("Images bytes buffer is null!")
                return null
            }
            val bytes = ByteArray(size!!.width * size!!.height * 3)
            val data = arrayOf(bytes)
            buffer[bytes]
            val dbuf = DataBufferByte(data, bytes.size, OFFSET)
            val raster = Raster.createWritableRaster(smodel, dbuf, null)
            val bi = BufferedImage(cmodel, raster, false, null)
            bi.flush()
            return bi
        }


    override fun open() {
        if (disposed.get()) {
            return
        }
        LOG.debug("Opening webcam device {}", name)
        if (size == null) {
            size = resolutions[0]
        }
        if (size == null) {
            throw RuntimeException("The resolution size cannot be null")
        }
        LOG.debug("Webcam device {} starting session, size {}", deviceRef.identifierStr, size)
        grabber = OpenIMAJGrabber()

        // NOTE!

        // Following the note from OpenIMAJ code - it seams like there is some
        // issue on 32-bit systems which prevents grabber to find devices.
        // According to the mentioned note this for loop shall fix the problem.
        val list = grabber!!.videoDevices!!.get()!!
        for (d in list.asArrayList()) {
            d.nameStr
            d.identifierStr
        }
        val started = grabber!!.startSession(
            size!!.width, size!!.height, 50, Pointer.pointerTo(
                deviceRef
            )
        )
        if (!started) {
            throw WebcamException("Cannot start native grabber!")
        }

        // set timeout, this MUST be done after grabber is open and before it's closed, otherwise it
        // will result as crash
        grabber!!.setTimeout(timeout)
        LOG.debug("Webcam device session started")
        val size2 = Dimension(grabber!!.width, grabber!!.height)
        val w1 = size!!.width
        val w2 = size2.width
        val h1 = size!!.height
        val h2 = size2.height
        if (w1 != w2 || h1 != h2) {
            if (failOnSizeMismatch) {
                throw WebcamException(
                    String.format(
                        "Different size obtained vs requested - [%dx%d] vs [%dx%d]",
                        w1,
                        h1,
                        w2,
                        h2
                    )
                )
            }
            val args = arrayOf<Any>(w1, h1, w2, h2, w2, h2)
            LOG.warn(
                "Different size obtained vs requested - [{}x{}] vs [{}x{}]. Setting correct one. New size is [{}x{}]",
                *args
            )
            size = Dimension(w2, h2)
        }
        smodel = ComponentSampleModel(DATA_TYPE, size!!.width, size!!.height, 3, size!!.width * 3, BAND_OFFSETS)

        // clear device memory buffer
        LOG.debug("Clear memory buffer")
        clearMemoryBuffer()

        // set device to open
        LOG.debug("Webcam device {} is now open", this)
        open.set(true)

        // start underlying frames refresher
        refresher = startFramesRefresher()
    }

    /**
     * this is to clean up all frames from device memory buffer which causes initial frames to be
     * completely blank (black images)
     */
    private fun clearMemoryBuffer() {
        for (i in 0 until DEVICE_BUFFER_SIZE) {
            grabber!!.nextFrame()
        }
    }

    /**
     * Start underlying frames refresher.
     *
     * @return Refresher thread
     */
    private fun startFramesRefresher(): Thread {
        val refresher = Thread(this, String.format("frames-refresher-[%s]", deviceId))
        refresher.uncaughtExceptionHandler = WebcamExceptionHandler.instance
        refresher.isDaemon = true
        refresher.start()
        return refresher
    }

    override fun close() {
        if (!open.compareAndSet(true, false)) {
            return
        }
        LOG.debug("Closing webcam device")
        grabber!!.stopSession()
    }

    override fun dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return
        }
        LOG.debug("Disposing webcam device {}", name)
        close()
    }

    /**
     * Determines if device should fail when requested image size is different than actually
     * received.
     *
     * @param fail the fail on size mismatch flag, true or false
     */
    fun setFailOnSizeMismatch(fail: Boolean) {
        failOnSizeMismatch = fail
    }

    override val isOpen: Boolean
        get() = open.get()

    /**
     * Get timeout for image acquisition.
     *
     * @return Value in milliseconds
     */
    fun getTimeout(): Int {
        return timeout
    }

    /**
     * Set timeout for image acquisition.
     *
     * @param timeout the timeout value in milliseconds
     */
    fun setTimeout(timeout: Int) {
        if (isOpen) {
            throw WebcamException("Timeout must be set before webcam is open")
        }
        this.timeout = timeout
    }

    /**
     * Update underlying memory buffer and fetch new frame.
     */
    private fun updateFrameBuffer() {
        LOG.trace("Next frame")
        if (t1 == -1L || t2 == -1L) {
            t1 = System.currentTimeMillis()
            t2 = System.currentTimeMillis()
        }
        val result = NextFrameTask(this).nextFrame()
        t1 = t2
        t2 = System.currentTimeMillis()
        fps = (4 * fps + 1000 / (t2 - t1 + 1)) / 5
        if (result == -1) {
            LOG.error("Timeout when requesting image!")
        } else if (result < -1) {
            LOG.error("Error requesting new frame!")
        }
    }

    override fun run() {
        do {
            if (Thread.interrupted()) {
                LOG.debug("Refresher has been interrupted")
                return
            }
            if (!open.get()) {
                LOG.debug("Cancelling refresher")
                return
            }
            updateFrameBuffer()
        } while (open.get())
    }

    companion object {
        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamDefaultDevice::class.java)

        /**
         * The device memory buffer size.
         */
        private const val DEVICE_BUFFER_SIZE = 5

        /**
         * Artificial view sizes. I'm really not sure if will fit into other webcams but hope that
         * OpenIMAJ can handle this.
         */
        private val DIMENSIONS = arrayOf(
            WebcamResolution.QQVGA.size,
            WebcamResolution.QVGA.size,
            WebcamResolution.VGA.size
        )

        /**
         * RGB offsets.
         */
        private val BAND_OFFSETS = intArrayOf(0, 1, 2)

        // Number of bits per component (RGB) for a pixel.
        private val BITS = intArrayOf(8, 8, 8)

        /**
         * Image offset.
         */
        private val OFFSET = intArrayOf(0)

        /**
         * Data type used in image.
         */
        private const val DATA_TYPE = DataBuffer.TYPE_BYTE

        /**
         * Image color space.
         */
        private val COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_sRGB)
    }
}