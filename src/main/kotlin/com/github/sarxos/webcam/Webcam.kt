package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamDevice.FPSSource
import com.github.sarxos.webcam.WebcamUpdater.DefaultDelayCalculator
import com.github.sarxos.webcam.WebcamUpdater.DelayCalculator
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver
import com.github.sarxos.webcam.ds.cgt.*
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Webcam class. It wraps webcam device obtained from webcam driver.
 *
 * @author Bartosz Firyn (bfiryn)
 */
class Webcam(private val device: WebcamDevice) {
    /**
     * Class used to asynchronously notify all webcam listeners about new image available.
     *
     * @author Bartosz Firyn (sarxos)
     */
    class ImageNotification
    /**
     * Create new notification.
     *
     * @param webcam the webcam from which image has been acquired
     * @param image the acquired image
     */(
        /**
         * Camera.
         */
        private val webcam: Webcam,
        /**
         * Acquired image.
         */
        private val image: BufferedImage?
    ) : Runnable {
        override fun run() {
            if (image != null) {
                val we = WebcamEvent(WebcamEventType.NEW_IMAGE, webcam, image)
                for (l in webcam.webcamListeners) {
                    try {
                        l.webcamImageObtained(we)
                    } catch (e: Exception) {
                        LOG.error(
                            String.format(
                                "Notify image acquired, exception when calling listener %s",
                                l.javaClass
                            ), e
                        )
                    }
                }
            }
        }
    }

    private inner class NotificationThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(r, String.format("notificator-[%s]", name))
            t.uncaughtExceptionHandler = WebcamExceptionHandler.instance
            t.isDaemon = true
            return t
        }
    }

    /**
     * Webcam listeners.
     */
    private val listeners: MutableList<WebcamListener> = CopyOnWriteArrayList()

    /**
     * List of custom resolution sizes supported by webcam instance.
     */
    private var customSizes: MutableList<Dimension> = ArrayList()

    /**
     * Shutdown hook.
     */
    private var hook: WebcamShutdownHook? = null

    /**
     * Is webcam open?
     */
    private val open: AtomicBoolean = AtomicBoolean(false)

    /**
     * Is webcam already disposed?
     */
    private val disposed: AtomicBoolean = AtomicBoolean(false)

    /**
     * Is non-blocking (asynchronous) access enabled?
     */
    @Volatile
    private var asynchronous = false

    /**
     * Current FPS.
     */
    @Volatile
    private var fps = 0.0

    /**
     * Webcam image updater.
     */
    @Volatile
    private var updater: WebcamUpdater? = null
    /**
     * Return image transformer.
     *
     * @return Transformer instance
     */
    /**
     * Set image transformer.
     *
     * @param transformer the transformer to be set
     */
    /**
     * Image transformer.
     */
    @Volatile
    var imageTransformer: WebcamImageTransformer? = null

    /**
     * Lock which denies access to the given webcam when it's already in use by other webcam capture
     * API process or thread.
     */
    var lock: WebcamLock = WebcamLock(this)

    /**
     * Executor service for image notifications.
     */
    private var notificator: ExecutorService? = null

    /**
     * Asynchronously start new thread which will notify all webcam listeners about the new image
     * available.
     */
    fun notifyWebcamImageAcquired(image: BufferedImage?) {

        // notify webcam listeners of new image available, do that only if there
        // are any webcam listeners available because there is no sense to start
        // additional threads for no purpose
        if (webcamListenersCount > 0) {
            notificator!!.execute(ImageNotification(this, image))
        }
    }
    /**
     * Open the webcam in either blocking (synchronous) or non-blocking (asynchronous) mode.The
     * difference between those two modes lies in the image acquisition mechanism.<br></br>
     * <br></br>
     * In blocking mode, when user calls [.getImage] method, device is being queried for new
     * image buffer and user have to wait for it to be available.<br></br>
     * <br></br>
     * In non-blocking mode, there is a special thread running in the background which constantly
     * fetch new images and cache them internally for further use. This cached instance is returned
     * every time when user request new image. Because of that it can be used when timeing is very
     * important, because all users calls for new image do not have to wait on device response. By
     * using this mode user should be aware of the fact that in some cases, when two consecutive
     * calls to get new image are executed more often than webcam device can serve them, the same
     * image instance will be returned. User should use [.isImageNew] method to distinguish
     * if returned image is not the same as the previous one. <br></br>
     * The background thread uses implementation of DelayCalculator interface to calculate delay
     * between two image fetching. Custom implementation may be specified as parameter of this
     * method. If the non-blocking mode is enabled and no DelayCalculator is specified,
     * DefaultDelayCalculator will be used.
     *
     * @param async true for non-blocking mode, false for blocking
     * @param delayCalculator responsible for calculating delay between two image fetching in
     * non-blocking mode; It's ignored in blocking mode.
     * @return True if webcam has been open
     * @throws WebcamException when something went wrong
     */
    /**
     * Open the webcam in either blocking (synchronous) or non-blocking (asynchronous) mode. If the
     * non-blocking mode is enabled the DefaultDelayCalculator is used for calculating delay between
     * two image fetching.
     *
     * @param async true for non-blocking mode, false for blocking
     * @return True if webcam has been open, false otherwise
     * @see .open
     * @throws WebcamException when something went wrong
     */
    /**
     * Open the webcam in blocking (synchronous) mode.
     *
     * @return True if webcam has been open, false otherwise
     * @see .open
     * @throws WebcamException when something went wrong
     */
    @JvmOverloads
    fun open(async: Boolean = false, delayCalculator: DelayCalculator? = DefaultDelayCalculator()): Boolean {
        if (open.compareAndSet(false, true)) {
            notificator = Executors.newSingleThreadExecutor(NotificationThreadFactory())

            // lock webcam for other Java (only) processes
            lock.lock()

            // open webcam device
            val task = WebcamOpenTask(driver, device)
            try {
                task.open()
            } catch (e: InterruptedException) {
                lock.unlock()
                open.set(false)
                LOG.debug("Thread has been interrupted in the middle of webcam opening process!", e)
                return false
            } catch (e: WebcamException) {
                lock.unlock()
                open.set(false)
                LOG.debug("Webcam exception when opening", e)
                throw e
            }
            LOG.debug("Webcam is now open {}", name)

            // install shutdown hook
            try {
                Runtime.getRuntime().addShutdownHook(WebcamShutdownHook(this).also { hook = it })
            } catch (e: IllegalStateException) {
                LOG.debug("Shutdown in progress, do not open device")
                LOG.trace(e.message, e)
                close()
                return false
            }

            // setup non-blocking configuration
            if (async.also { asynchronous = it }) {
                if (updater == null) {
                    updater = WebcamUpdater(this, delayCalculator)
                }
                updater!!.start()
            }

            // notify listeners
            val we = WebcamEvent(WebcamEventType.OPEN, this)
            val wli: Iterator<WebcamListener> = listeners.iterator()
            var l: WebcamListener?
            while (wli.hasNext()) {
                l = wli.next()
                try {
                    l.webcamOpen(we)
                } catch (e: Exception) {
                    LOG.error(String.format("Notify webcam open, exception when calling listener %s", l.javaClass), e)
                }
            }
        } else {
            LOG.debug("Webcam is already open {}", name)
        }
        return true
    }

    /**
     * Close the webcam.
     *
     * @return True if webcam has been open, false otherwise
     */
    fun close(): Boolean {
        if (open.compareAndSet(true, false)) {
            LOG.debug("Closing webcam {}", name)

            // close webcam
            val task = WebcamCloseTask(driver, device)
            try {
                task.close()
            } catch (e: InterruptedException) {
                open.set(true)
                LOG.debug("Thread has been interrupted before webcam was closed!", e)
                return false
            } catch (e: WebcamException) {
                open.set(true)
                throw e
            }

            // stop updater
            if (asynchronous) {
                updater!!.stop()
            }

            // remove shutdown hook (it's not more necessary)
            removeShutdownHook()

            // unlock webcam so other Java processes can start using it
            lock.unlock()

            // notify listeners
            val we = WebcamEvent(WebcamEventType.CLOSED, this)
            for (listener in listeners) {
                try {
                    listener.webcamClosed(we)

                } catch (e: Exception) {
                    LOG.error(
                        String.format(
                            "Notify webcam closed, exception when calling %s listener",
                            listener.javaClass
                        ), e
                    )
                }
            }
            notificator!!.shutdown()
            while (!notificator!!.isTerminated) {
                try {
                    notificator!!.awaitTermination(100, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    return false
                }
            }
            LOG.debug("Webcam {} has been closed", name)
        } else {
            LOG.debug("Webcam {} is already closed", name)
        }
        return true
    }

    /**
     * Return underlying webcam device. Depending on the driver used to discover devices, this
     * method can return instances of different class. By default [WebcamDefaultDevice] is
     * returned when no external driver is used.
     *
     * @return Underlying webcam device instance
     */
    fun getDevice(): WebcamDevice {
        return device
    }

    /**
     * Completely dispose capture device. After this operation webcam cannot be used any more and
     * full reinstantiation is required.
     */
    fun dispose() {
        assert(driver != null)
        if (!disposed.compareAndSet(false, true)) {
            return
        }
        open.set(false)
        lock.unlock()
        LOG.info("Disposing webcam {}", name)
        val task = WebcamDisposeTask(driver, device)
        try {
            task.dispose()
        } catch (e: InterruptedException) {
            LOG.error("Processor has been interrupted before webcam was disposed!", e)
            return
        }
        val we = WebcamEvent(WebcamEventType.DISPOSED, this)
        for (listener in listeners) {
            try {
                listener.webcamClosed(we)
                listener.webcamDisposed(we)
            } catch (e: Exception) {
                LOG.error(String.format("Notify webcam disposed, exception when calling %s listener", listener.javaClass), e)
            }
        }
        removeShutdownHook()
        LOG.debug("Webcam disposed {}", name)
    }

    private fun removeShutdownHook() {

        // hook can be null because there is a possibility that webcam has never
        // been open and therefore hook was not created
        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook)
            } catch (e: IllegalStateException) {
                LOG.trace("Shutdown in progress, cannot remove hook")
            }
        }
    }

    /**
     * TRansform image using image transformer. If image transformer has not been set, this method
     * return instance passed in the argument, without any modifications.
     *
     * @param image the image to be transformed
     * @return Transformed image (if transformer is set)
     */
    fun transform(image: BufferedImage?): BufferedImage? {
        if (image != null) {
            if (imageTransformer != null) {
                return imageTransformer!!.transform(image)
            }
        }
        return image
    }

    /**
     * Is webcam open?
     *
     * @return true if open, false otherwise
     */
    fun isOpen(): Boolean {
        return open.get()
    }
    /**
     * Get current webcam resolution in pixels.
     *
     * @return Webcam resolution (picture size) in pixels.
     */// check if new resolution is the same as current one

    // check if new resolution is valid
    /**
     * Set new view size. New size has to exactly the same as one of the default sized or exactly
     * the same as one of the custom ones.
     *
     * @param size the new view size to be set
     * @see Webcam.setCustomViewSizes
     * @see Webcam.getViewSizes
     */
    var viewSize: Dimension
        get() = device.getResolution()
        set(size) {
            check(!open.get()) { "Cannot change resolution when webcam is open, please close it first" }

            // check if new resolution is the same as current one
            val current = viewSize
            if (current.width == size.width && current.height == size.height) {
                return
            }

            // check if new resolution is valid
            val predefined = viewSizes
            val custom = customViewSizes
            var ok = false
            for (d in predefined) {
                if (d.width == size.width && d.height == size.height) {
                    ok = true
                    break
                }
            }
            if (!ok) {
                for (d in custom) {
                    if (d.width == size.width && d.height == size.height) {
                        ok = true
                        break
                    }
                }
            }
            if (!ok) {
                val sb = StringBuilder("Incorrect dimension [")
                sb.append(size.width).append("x").append(size.height).append("] ")
                sb.append("possible ones are ")
                for (d in predefined) {
                    sb.append("[").append(d.width).append("x").append(d.height).append("] ")
                }
                for (d in custom) {
                    sb.append("[").append(d.width).append("x").append(d.height).append("] ")
                }
                throw IllegalArgumentException(sb.toString())
            }
            LOG.debug("Setting new resolution {}x{}", size.width, size.height)
            device.setResolution(size)
        }

    /**
     * Return list of supported view sizes. It can differ between vary webcam data sources.
     *
     * @return Array of supported dimensions
     */
    val viewSizes: Array<Dimension>
        get() = device.resolutions

    /**
     * Set custom resolution. If you are using this method you have to make sure that your webcam
     * device can support this specific resolution.
     *
     * @param sizes the array of custom resolutions to be supported by webcam
     */
    var customViewSizes: Array<Dimension>
        get() {
            return customSizes.toTypedArray()
        }
        set(sizes) {
            setCustomViewSizess(*sizes)
        }// +1 to avoid division by zero

    fun setCustomViewSizess(vararg sizes: Dimension) {
        customSizes = mutableListOf(*sizes)
    }

    // notify webcam listeners about new image available
// get image

    // get FPS
    /**
     * Capture image from webcam and return it. Will return image object or null if webcam is closed
     * or has been already disposed by JVM.<br></br>
     * <br></br>
     * **IMPORTANT NOTE!!!**<br></br>
     * <br></br>
     * There are two possible behaviors of what webcam should do when you try to get image and
     * webcam is actually closed. Normally it will return null, but there is a special flag which
     * can be statically set to switch all webcams to auto open mode. In this mode, webcam will be
     * automatically open, when you try to get image from closed webcam. Please be aware of some
     * side effects! In case of multi-threaded applications, there is no guarantee that one thread
     * will not try to open webcam even if it was manually closed in different thread.
     *
     * @return Captured image or null if webcam is closed or disposed by JVM
     */
    val image: BufferedImage?
        get() {
            if (!isReady) {
                return null
            }
            val t1: Long
            val t2: Long
            return if (asynchronous) {
                updater!!.getImage()
            } else {

                // get image
                t1 = System.currentTimeMillis()
                val image = transform(WebcamGetImageTask(driver, device).image)
                t2 = System.currentTimeMillis()
                if (image == null) {
                    return null
                }

                // get FPS
                fps = if (device is FPSSource) {
                    (device as FPSSource).fps
                } else {
                    // +1 to avoid division by zero
                    (4 * fps + 1000 / (t2 - t1 + 1)) / 5
                }

                // notify webcam listeners about new image available
                notifyWebcamImageAcquired(image)
                image
            }
        }

    val preallocatedImageBytes : ByteArray get() = device.preallocatedImageBytes

    val fPS: Double
        get() = if (asynchronous) {
            updater!!.fPS
        } else {
            fps
        }// some devices can support direct image buffers, and for those call



    // processor task, and for those which does not support direct image
    // buffers, just convert image to RGB byte array
    /**
     * Get RAW image ByteBuffer. It will always return buffer with 3 x 1 bytes per each pixel, where
     * RGB components are on (0, 1, 2) and color space is sRGB.<br></br>
     * <br></br>
     * **IMPORTANT!**<br></br>
     * Some drivers can return direct ByteBuffer, so there is no guarantee that underlying bytes
     * will not be released in next read image operation. Therefore, to avoid potential bugs you
     * should convert this ByteBuffer to bytes array before you fetch next image.
     *
     * @return Byte buffer
     */
    val imageByteBuffer: ByteBuffer?
        get() {
            if (!isReady) {
                return null
            }
            assert(driver != null)
            val t1: Long
            val t2: Long

            // some devices can support direct image buffers, and for those call
            // processor task, and for those which does not support direct image
            // buffers, just convert image to RGB byte array
            return if (device is WebcamDevice.BufferAccess) {
                t1 = System.currentTimeMillis()
                try {
                    WebcamGetBufferTask(driver, device).buffer
                } finally {
                    t2 = System.currentTimeMillis()
                    if (device is FPSSource) {
                        fps = (device as FPSSource).fps
                    } else {
                        fps = (4 * fps + 1000 / (t2 - t1 + 1)) / 5
                    }
                }
            } else {
                throw IllegalStateException(
                    String.format(
                        "Driver %s does not support buffer access",
                        driver!!.javaClass.name
                    )
                )
            }
        }

    /**
     * Get RAW image ByteBuffer. It will always return buffer with 3 x 1 bytes per each pixel, where
     * RGB components are on (0, 1, 2) and color space is sRGB.<br></br>
     * <br></br>
     * **IMPORTANT!**<br></br>
     * Some drivers can return direct ByteBuffer, so there is no guarantee that underlying bytes
     * will not be released in next read image operation. Therefore, to avoid potential bugs you
     * should convert this ByteBuffer to bytes array before you fetch next image.
     *
     * @param target the target [ByteBuffer] object to copy data into
     */
    fun getImageBytes(target: ByteBuffer?) {
        if (!isReady) {
            return
        }
        assert(driver != null)
        val t1: Long
        val t2: Long

        // some devices can support direct image buffers, and for those call
        // processor task, and for those which does not support direct image
        // buffers, just convert image to RGB byte array
        if (device is WebcamDevice.BufferAccess) {
            t1 = System.currentTimeMillis()
            try {
                WebcamReadBufferTask(driver, device, target).readBuffer()
            } finally {
                t2 = System.currentTimeMillis()
                if (device is FPSSource) {
                    fps = (device as FPSSource).fps
                } else {
                    fps = (4 * fps + 1000 / (t2 - t1 + 1)) / 5
                }
            }
        } else {
            throw IllegalStateException(
                String.format(
                    "Driver %s does not support buffer access",
                    driver!!.javaClass.name
                )
            )
        }
    }

    /**
     * If the underlying device implements Configurable interface, specified parameters are passed
     * to it. May be called before the open method or later in dependence of the device
     * implementation.
     *
     * @param parameters - Map of parameters changing device defaults
     * @see Configurable
     */
    fun setParameters(parameters: Map<String?, *>?) {
        val device = getDevice()
        if (device is WebcamDevice.Configurable) {
            (device as WebcamDevice.Configurable).setParameters(parameters)
        } else {
            LOG.debug("Webcam device {} is not configurable", device)
        }
    }

    /**
     * Is webcam ready to be read.
     *
     * @return True if ready, false otherwise
     */
    private val isReady: Boolean
        get() {
            if (disposed.get()) {
                LOG.warn("Cannot get image, webcam has been already disposed")
                return false
            }
            if (!open.get()) {
                if (isAutoOpenMode) {
                    open()
                } else {
                    return false
                }
            }
            return true
        }

    /**
     * Get webcam name (device name). The name of device depends on the value returned by the
     * underlying data source, so in some cases it can be human-readable value and sometimes it can
     * be some strange number.
     *
     * @return Name
     */
    val name: String
        get() = device.name

    override fun toString(): String {
        return String.format("Webcam %s", name)
    }

    /**
     * Add webcam listener.
     *
     * @param l the listener to be added
     * @return True if listener has been added, false if it was already there
     * @throws IllegalArgumentException when argument is null
     */
    fun addWebcamListener(l: WebcamListener): Boolean {
        return listeners.add(l)
    }

    /**
     * @return All webcam listeners
     */
    val webcamListeners: Array<WebcamListener>
        get() {
            return listeners.toTypedArray()
        }

    /**
     * @return Number of webcam listeners
     */
    private val webcamListenersCount: Int
        get() {
            return listeners.size
        }

    /**
     * Removes webcam listener.
     *
     * @param l the listener to be removed
     * @return True if listener has been removed, false otherwise
     */
    fun removeWebcamListener(l: WebcamListener): Boolean {
        return listeners.remove(l)
    }

    companion object {
        /**
         * Logger instance.
         */
        private val LOG = LoggerFactory.getLogger(Webcam::class.java)!!

        /**
         * List of driver classes names to search for.
         */
        private val DRIVERS_LIST: MutableList<String> = ArrayList()

        /**
         * List of driver classes to search for.
         */
        private val DRIVERS_CLASS_LIST: MutableList<Class<*>> = ArrayList()

        /**
         * Discovery listeners.
         */
        private val DISCOVERY_LISTENERS = Collections.synchronizedList(ArrayList<WebcamDiscoveryListener>())

        /**
         * Webcam driver (LtiCivil, JMF, FMJ, JQT, OpenCV, VLCj, etc).
         */
        @Volatile
        private var driver: WebcamDriver? = null
        /**
         * Return discovery service without creating it if not exists.
         *
         * @return Discovery service or null if not yet created
         */


        /**
         * Is automated deallocation on TERM signal enabled.
         */
        private var deallocOnTermSignal = false
        /**
         * Is auto open mode enabled. Auto open mode will will automatically open webcam whenever user
         * will try to get image from instance which has not yet been open. Please be aware of some side
         * effects! In case of multi-threaded applications, there is no guarantee that one thread will
         * not try to open webcam even if it was manually closed in different thread.
         *
         * @return True if mode is enabled, false otherwise
         */
        /**
         * Switch all webcams to auto open mode. In this mode, each webcam will be automatically open
         * whenever user will try to get image from instance which has not yet been open. Please be
         * aware of some side effects! In case of multi-threaded applications, there is no guarantee
         * that one thread will not try to open webcam even if it was manually closed in different
         * thread.
         *
         * @param on true to enable, false to disable
         */
        /**
         * Is auto-open feature enabled?
         */
        var isAutoOpenMode = false
        // wait around three hundreds billion years for it to occur
        /**
         * Get list of webcams to use. This method will wait predefined time interval for webcam devices
         * to be discovered. By default this time is set to 1 minute.
         *
         * @return List of webcams existing in the system
         * @throws WebcamException when something is wrong
         * @see Webcam.getWebcams
         */
        @get:Throws(WebcamException::class)
        val webcams: List<Webcam>
            get() =// timeout exception below will never be caught since user would have to
                // wait around three hundreds billion years for it to occur
                try {
                    getWebcams(NonNegativeTimeout.MAX)
                } catch (e: TimeoutException) {
                    throw RuntimeException(e)
                }

        /**
         * Get list of webcams to use. This method will wait given time interval for webcam devices to
         * be discovered.
         *
         * @param timeout the devices discovery timeout
         * @param timeUnit the time unit
         * @return List of webcams
         * @throws TimeoutException when timeout has been exceeded
         * @throws WebcamException when something is wrong
         * @throws IllegalArgumentException when timeout is negative or tunit null
         */
        @Synchronized
        @Throws(TimeoutException::class, WebcamException::class)
        fun getWebcams(timeout: NonNegativeTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): List<Webcam> {
            val discovery = discoveryService
            val webcams = discovery.getWebcams(timeout, timeUnit)
            if (!discovery.isRunning()) {
                discovery.start()
            }
            return webcams
        }

        /**
         * Will discover and return first webcam available in the system.
         *
         * @param timeout the webcam discovery timeout (1 minute by default)
         * @return Default webcam (first from the list)
         * @throws TimeoutException when discovery timeout has been exceeded
         * @throws WebcamException if something is really wrong
         * @throws IllegalArgumentException when timeout is negative
         * @see Webcam.getWebcams
         */
        @Throws(TimeoutException::class, WebcamException::class)
        fun getDefault(timeout: NonNegativeTimeout = NonNegativeTimeout.MAX): Webcam? {
            return getDefault(timeout, TimeUnit.MILLISECONDS)
        }

        /**
         * Will discover and return first webcam available in the system.
         *
         * @param timeout the webcam discovery timeout (1 minute by default)
         * @param tunit the time unit
         * @return Default webcam (first from the list)
         * @throws TimeoutException when discovery timeout has been exceeded
         * @throws WebcamException if something is really wrong
         * @throws IllegalArgumentException when timeout is negative or tunit null
         * @see Webcam.getWebcams
         */
        @Throws(TimeoutException::class, WebcamException::class)
        fun getDefault(timeout: NonNegativeTimeout, tunit: TimeUnit): Webcam? {
            val webcams = getWebcams(timeout, tunit)
            if (webcams.isNotEmpty()) {
                return webcams[0]
            }
            LOG.warn("No webcam has been detected!")
            return null
        }

        /**
         * Return webcam driver. Perform search if necessary.<br></br>
         * <br></br>
         * **This method is not thread-safe!**
         *
         * @return Webcam driver
         */
        @JvmStatic
        @Synchronized
        fun getDriver(): WebcamDriver {
            if (driver != null) {
                return driver!!
            }
            if (driver == null) {
                driver = WebcamDriverUtils.findDriver(DRIVERS_LIST, DRIVERS_CLASS_LIST)
            }
            if (driver == null) {
                driver = WebcamDefaultDriver()
            }
            LOG.info("{} capture driver will be used", driver!!.javaClass.simpleName)
            return driver!!
        }

        /**
         * Set new video driver to be used by webcam.<br></br>
         * <br></br>
         * **This method is not thread-safe!**
         *
         * @param wd new webcam driver to be used (e.g. LtiCivil, JFM, FMJ, QTJ)
         * @throws IllegalArgumentException when argument is null
         */
        fun setDriver(wd: WebcamDriver) {
            LOG.debug("Setting new capture driver {}", wd)
            resetDriver()
            driver = wd
        }

        /**
         * Set new video driver class to be used by webcam. Class given in the argument shall extend
         * [WebcamDriver] interface and should have public default constructor, so instance can be
         * created by reflection.<br></br>
         * <br></br>
         * **This method is not thread-safe!**
         *
         * @param driverClass new video driver class to use
         * @throws IllegalArgumentException when argument is null
         */
        fun setDriver(driverClass: Class<out WebcamDriver>) {
            resetDriver()
            try {
                driver = driverClass.newInstance()
            } catch (e: InstantiationException) {
                throw WebcamException(e)
            } catch (e: IllegalAccessException) {
                throw WebcamException(e)
            }
        }

        /**
         * Reset webcam driver.<br></br>
         * <br></br>
         * **This method is not thread-safe!**
         */
        fun resetDriver() {
            synchronized(DRIVERS_LIST) { DRIVERS_LIST.clear() }
            if (discoveryServiceRef != null) {
                discoveryServiceRef!!.shutdown()
                discoveryServiceRef = null
            }
            driver = null
        }

        /**
         * Register new webcam video driver.
         *
         * @param clazz webcam video driver class
         * @throws IllegalArgumentException when argument is null
         */
        fun registerDriver(clazz: Class<out WebcamDriver?>) {
            DRIVERS_CLASS_LIST.add(clazz)
            registerDriver(clazz.canonicalName)
        }

        /**
         * Register new webcam video driver.
         *
         * @param clazzName webcam video driver class name
         * @throws IllegalArgumentException when argument is null
         */
        fun registerDriver(clazzName: String) {
            DRIVERS_LIST.add(clazzName)
        }
        /**
         * Is TERM signal handler enabled.
         *
         * @return True if enabled, false otherwise
         */
        /**
         * **CAUTION!!!**<br></br>
         * <br></br>
         * This is experimental feature to be used mostly in in development phase. After you set handle
         * term signal to true, and fetch capture devices, Webcam Capture API will listen for TERM
         * signal and try to close all devices after it has been received. **This feature can be
         * unstable on some systems!**
         *
         * @param on signal handling will be enabled if true, disabled otherwise
         */
        var isHandleTermSignal: Boolean
            get() = deallocOnTermSignal
            set(on) {
                if (on) {
                    LOG.warn("Automated deallocation on TERM signal is now enabled! Make sure to not use it in production!")
                }
                deallocOnTermSignal = on
            }

        @JvmStatic
        val discoveryListeners: Array<WebcamDiscoveryListener>
            get() = DISCOVERY_LISTENERS.toTypedArray()

        /**
         * Webcam discovery service.
         */
        @get:Synchronized
        @Volatile
        var discoveryServiceRef: WebcamDiscoveryService? = null
            private set

        /**
         * Return discovery service.
         *
         * @return Discovery service
         */
        @get:Synchronized
        val discoveryService: WebcamDiscoveryService
            get() = if (discoveryServiceRef == null) {
                discoveryServiceRef = WebcamDiscoveryService(getDriver())
                discoveryServiceRef
            } else {
                discoveryServiceRef
            }!!

        /**
         * Return webcam with given name or null if no device with given name has been found. Please
         * note that specific webcam name may depend on the order it was connected to the USB port (e.g.
         * /dev/video0 vs /dev/video1).
         *
         * @param name the webcam name
         * @return Webcam with given name or null if not found
         * @throws IllegalArgumentException when name is null
         */
        fun getWebcamByName(name: String): Webcam? {
            return webcams.firstOrNull { it.name == name }
        }
    }
}