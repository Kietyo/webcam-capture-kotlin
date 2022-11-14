package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamExceptionHandler.Companion.handle
import com.github.sarxos.webcam.WebcamPanel
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.SwingWorker

/**
 * Simply implementation of JPanel allowing users to render pictures taken with webcam.
 *
 * @author Bartosz Firyn (SarXos)
 */
class WebcamPanel @JvmOverloads constructor(
    webcam: Webcam,
    size: Dimension?,
    start: Boolean,
    supplier: ImageSupplier = DefaultImageSupplier(webcam)
) : JPanel(), WebcamListener, PropertyChangeListener {
    /**
     * This enum is to control of how image will be drawn in the panel bounds.
     *
     * @author Sylwia Kauczor
     */
    enum class DrawMode {
        /**
         * Do not resize image - paint it as it is. This will make the image to go off out the
         * bounds if panel is smaller than image size.
         */
        NONE,

        /**
         * Will resize image to the panel bounds. This mode does not care of the image scale, so the
         * final image may be disrupted.
         */
        FILL,

        /**
         * Will fir image into the panel bounds. This will resize the image and keep both x and y
         * scale factor.
         */
        FIT
    }

    /**
     * This interface can be used to supply [BufferedImage] to [WebcamPanel].
     *
     * @author Bartosz Firyn (sarxos)
     */
    interface ImageSupplier {
        /**
         * @return [BufferedImage] to be displayed in [WebcamPanel]
         */
        fun get(): BufferedImage?
    }

    /**
     * Default implementation of [ImageSupplier] used in [WebcamPanel]. It invokes
     * [Webcam.getImage] and return [BufferedImage].
     *
     * @author Bartosz Firyn (sarxos)
     */
    private class DefaultImageSupplier(private val webcam: Webcam?) : ImageSupplier {
        override fun get(): BufferedImage? {
            return webcam!!.image
        }
    }

    /**
     * Interface of the painter used to draw image in panel.
     *
     * @author Bartosz Firyn (SarXos)
     */
    interface Painter {
        /**
         * Paint panel without image.
         *
         * @param panel the webcam panel to paint on
         * @param g2 the graphics 2D object used for drawing
         */
        fun paintPanel(panel: WebcamPanel?, g2: Graphics2D?)

        /**
         * Paint webcam image in panel.
         *
         * @param panel the webcam panel to paint on
         * @param image the image from webcam
         * @param g2 the graphics 2D object used for drawing
         */
        fun paintImage(panel: WebcamPanel?, image: BufferedImage?, g2: Graphics2D?)
    }

    /**
     * Default painter used to draw image in panel.
     *
     * @author Bartosz Firyn (SarXos)
     * @author Sylwia Kauczor
     */
    inner class DefaultPainter : Painter {
        /**
         * Webcam device name.
         */
        private var name: String? = null

        /**
         * Lat repaint time, uset for debug purpose.
         */
        private var lastRepaintTime: Long = -1

        /**
         * Buffered image resized to fit into panel drawing area.
         */
        private var resizedImage: BufferedImage? = null
        override fun paintPanel(panel: WebcamPanel?, g2: Graphics2D?) {
            assert(panel != null)
            assert(g2 != null)
            val antialiasing = g2!!.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                if (isAntialiasingEnabled) RenderingHints.VALUE_ANTIALIAS_ON else RenderingHints.VALUE_ANTIALIAS_OFF
            )
            g2.background = Color.BLACK
            g2.fillRect(0, 0, width, height)
            val cx = (width - 70) / 2
            val cy = (height - 40) / 2
            g2.stroke = BasicStroke(2f)
            g2.color = Color.LIGHT_GRAY
            g2.fillRoundRect(cx, cy, 70, 40, 10, 10)
            g2.color = Color.WHITE
            g2.fillOval(cx + 5, cy + 5, 30, 30)
            g2.color = Color.LIGHT_GRAY
            g2.fillOval(cx + 10, cy + 10, 20, 20)
            g2.color = Color.WHITE
            g2.fillOval(cx + 12, cy + 12, 16, 16)
            g2.fillRoundRect(cx + 50, cy + 5, 15, 10, 5, 5)
            g2.fillRect(cx + 63, cy + 25, 7, 2)
            g2.fillRect(cx + 63, cy + 28, 7, 2)
            g2.fillRect(cx + 63, cy + 31, 7, 2)
            g2.color = Color.DARK_GRAY
            g2.stroke = BasicStroke(3f)
            g2.drawLine(0, 0, width, height)
            g2.drawLine(0, height, width, 0)
            var str: String?
            val strInitDevice = rb!!.getString("INITIALIZING_DEVICE")
            val strNoImage = rb!!.getString("NO_IMAGE")
            val strDeviceError = rb!!.getString("DEVICE_ERROR")
            str = if (isErrored) {
                strDeviceError
            } else {
                if (isStarting) strInitDevice else strNoImage
            }
            val metrics = g2.getFontMetrics(font)
            var w = metrics.stringWidth(str)
            var h = metrics.height
            val x = (width - w) / 2
            val y = cy - h
            g2.font = font
            g2.color = Color.WHITE
            g2.drawString(str, x, y)
            if (name == null) {
                name = webcam.name
            }
            str = name
            w = metrics.stringWidth(str)
            h = metrics.height
            g2.drawString(str, (width - w) / 2, cy - 2 * h)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing)
        }

        override fun paintImage(panel: WebcamPanel?, image: BufferedImage?, g2: Graphics2D?) {
            assert(panel != null)
            assert(image != null)
            assert(g2 != null)
            val pw = width
            val ph = height
            val iw = image!!.width
            val ih = image.height
            val antialiasing = g2!!.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
            val rendering = g2.getRenderingHint(RenderingHints.KEY_RENDERING)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
            g2.background = Color.BLACK
            g2.color = Color.BLACK
            g2.fillRect(0, 0, pw, ph)

            // resized image position and size
            var x = 0
            var y = 0
            var w = 0
            var h = 0
            when (drawMode) {
                DrawMode.NONE -> {
                    w = image.width
                    h = image.height
                }

                DrawMode.FILL -> {
                    w = pw
                    h = ph
                }

                DrawMode.FIT -> {
                    val s = Math.max(iw.toDouble() / pw, ih.toDouble() / ph)
                    val niw = iw / s
                    val nih = ih / s
                    val dx = (pw - niw) / 2
                    val dy = (ph - nih) / 2
                    w = niw.toInt()
                    h = nih.toInt()
                    x = dx.toInt()
                    y = dy.toInt()
                }
            }
            if (resizedImage != null) {
                resizedImage!!.flush()
            }
            if (w == image.width && h == image.height && !isMirrored) {
                resizedImage = image
            } else {
                val genv = GraphicsEnvironment.getLocalGraphicsEnvironment()
                val gc = genv.defaultScreenDevice.defaultConfiguration
                var gr: Graphics2D? = null
                try {
                    resizedImage = gc.createCompatibleImage(pw, ph)
                    gr = resizedImage!!.createGraphics()
                    gr.composite = AlphaComposite.Src
                    for ((key, value) in imageRenderingHints) {
                        gr.setRenderingHint(key, value)
                    }
                    gr.background = Color.BLACK
                    gr.color = Color.BLACK
                    gr.fillRect(0, 0, pw, ph)
                    val sx1: Int
                    val sx2: Int
                    val sy1: Int
                    val sy2: Int // source rectangle coordinates
                    val dx1: Int
                    val dx2: Int
                    val dy1: Int
                    val dy2: Int // destination rectangle coordinates
                    dx1 = x
                    dy1 = y
                    dx2 = x + w
                    dy2 = y + h
                    if (isMirrored) {
                        sx1 = iw
                        sy1 = 0
                        sx2 = 0
                        sy2 = ih
                    } else {
                        sx1 = 0
                        sy1 = 0
                        sx2 = iw
                        sy2 = ih
                    }
                    gr.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null)
                } finally {
                    gr?.dispose()
                }
            }
            g2.drawImage(resizedImage, 0, 0, null)
            if (isFPSDisplayed) {
                val str = String.format("FPS: %.1f", webcam.fPS)
                val sx = 5
                val sy = ph - 5
                g2.font = font
                g2.color = Color.BLACK
                g2.drawString(str, sx + 1, sy + 1)
                g2.color = Color.WHITE
                g2.drawString(str, sx, sy)
            }
            if (isImageSizeDisplayed) {
                val res = String.format("%d\u2A2F%d px", iw, ih)
                val metrics = g2.getFontMetrics(font)
                val sw = metrics.stringWidth(res)
                val sx = pw - sw - 5
                val sy = ph - 5
                g2.font = font
                g2.color = Color.BLACK
                g2.drawString(res, sx + 1, sy + 1)
                g2.color = Color.WHITE
                g2.drawString(res, sx, sy)
            }
            if (isDisplayDebugInfo) {
                if (lastRepaintTime < 0) {
                    lastRepaintTime = System.currentTimeMillis()
                } else {
                    val now = System.currentTimeMillis()
                    val res = String.format("DEBUG: repaints per second: %.1f", 1000.0 / (now - lastRepaintTime))
                    lastRepaintTime = now
                    g2.font = font
                    g2.color = Color.BLACK
                    g2.drawString(res, 6, 16)
                    g2.color = Color.WHITE
                    g2.drawString(res, 5, 15)
                }
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, rendering)
        }
    }

    private class PanelThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(r, String.format("webcam-panel-scheduled-executor-%d", number.incrementAndGet()))
            t.uncaughtExceptionHandler = WebcamExceptionHandler.instance
            t.isDaemon = true
            return t
        }

        companion object {
            private val number = AtomicInteger(0)
        }
    }

    /**
     * This runnable will do nothing more than repaint panel.
     */
    private class SwingRepainter(private val panel: WebcamPanel?) : Runnable {
        override fun run() {
            panel!!.repaint()
        }
    }

    /**
     * This runnable will do nothing more than repaint panel.
     */
    private val repaint: Runnable = SwingRepainter(this)
    /**
     * Hints for rendering, mainly used for custom painters
     *
     * @return the stored RenderingHints
     */
    /**
     * Rendering hints to be used when painting image to be displayed.
     */
    @get:Deprecated("use {@link #getDrawMode()} instead.")
    val imageRenderingHints: Map<RenderingHints.Key?, Any?> = HashMap(DEFAULT_IMAGE_RENDERING_HINTS)

    /**
     * Scheduled executor acting as timer.
     */
    private var executor: ScheduledExecutorService? = null

    /**
     * Image updater reads images from camera and force panel to be repainted.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private inner class ImageUpdater : Runnable {
        /**
         * Repaint scheduler schedule panel updates.
         *
         * @author Bartosz Firyn (sarxos)
         */
        private inner class RepaintScheduler : Thread() {
            /**
             * Repaint scheduler schedule panel updates.
             */
            init {
                uncaughtExceptionHandler = WebcamExceptionHandler.instance
                name = String.format("repaint-scheduler-%s", webcam.name)
                isDaemon = true
            }

            override fun run() {

                // do nothing when not running
                if (!running.get()) {
                    return
                }
                repaintPanel()

                // loop when starting, to wait for images
                while (isStarting) {
                    try {
                        sleep(50)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }

                // schedule update when webcam is open, otherwise schedule
                // second scheduler execution
                try {

                    // FPS limit means that panel rendering frequency is
                    // limited to the specific value and panel will not be
                    // rendered more often then specific value
                    if (webcam.isOpen()) {

                        // TODO: rename FPS value in panel to rendering
                        // frequency
                        if (isFPSLimited) {
                            executor!!.scheduleAtFixedRate(
                                updater,
                                0,
                                (1000 / frequency).toLong(),
                                TimeUnit.MILLISECONDS
                            )
                        } else {
                            executor!!.scheduleWithFixedDelay(updater, 100, 1, TimeUnit.MILLISECONDS)
                        }
                    } else {
                        executor!!.schedule(this, 500, TimeUnit.MILLISECONDS)
                    }
                } catch (e: RejectedExecutionException) {

                    // executor has been shut down, which means that someone
                    // stopped panel / webcam device before it was actually
                    // completely started (it was in "starting" timeframe)
                    LOG.warn("Executor rejected paint update")
                    LOG.trace("Executor rejected paint update because of", e)
                }
            }
        }

        /**
         * Update scheduler thread.
         */
        private var scheduler: Thread? = null

        /**
         * Is repainter running?
         */
        private val running = AtomicBoolean(false)

        /**
         * Start repainter. Can be invoked many times, but only first call will take effect.
         */
        fun start() {
            if (running.compareAndSet(false, true)) {
                executor = Executors.newScheduledThreadPool(1, THREAD_FACTORY)
                scheduler = RepaintScheduler()
                scheduler!!.start()
            }
        }

        /**
         * Stop repainter. Can be invoked many times, but only first call will take effect.
         *
         * @throws InterruptedException
         */
        @Throws(InterruptedException::class)
        fun stop() {
            if (running.compareAndSet(true, false)) {
                executor!!.shutdown()
                executor!!.awaitTermination(5000, TimeUnit.MILLISECONDS)
                scheduler!!.join()
            }
        }

        override fun run() {
            try {
                update()
            } catch (t: Throwable) {
                isErrored = true
                handle(t)
            }
        }

        /**
         * Perform single panel area update (repaint newly obtained image).
         */
        private fun update() {

            // do nothing when updater not running, when webcam is closed, or
            // panel repainting is paused
            if (!running.get() || !webcam.isOpen() || paused) {
                return
            }

            // get new image from webcam
            val tmp = supplier.get()
            var repaint = true
            if (tmp != null) {

                // ignore repaint if image is the same as before
                if (image === tmp) {
                    repaint = false
                }
                isErrored = false
                image = tmp
            }
            if (repaint) {
                repaintPanel()
            }
        }
    }

    /**
     * Resource bundle.
     */
    private var rb: ResourceBundle? = null
    /**
     * This method returns the current draw mode, mainly used by custom painters
     *
     * @return the current value of the [DrawMode]
     */
    /**
     * This method sets the drawmode
     *
     * @param drawMode the desired [DrawMode]
     */
    /**
     * The mode of how the image will be resized to fit into panel bounds. Default is
     * [DrawMode.FIT]
     *
     * @see DrawMode
     */
    var drawMode = DrawMode.FIT

    /**
     * Frames requesting frequency.
     */
    private var frequency = 5.0 // FPS
    /**
     * Is frequency limit enabled?
     *
     * @return True or false
     */
    /**
     * Enable or disable frequency limit. Frequency limit should be used for **all IP cameras
     * working in pull mode** (to save number of HTTP requests). If true, images will be fetched
     * in configured time intervals. If false, images will be fetched as fast as camera can serve
     * them.
     *
     * @param frequencyLimit true if limiting the frequency of image requests
     */
    /**
     * Is frames requesting frequency limited? If true, images will be fetched in configured time
     * intervals. If false, images will be fetched as fast as camera can serve them.
     */
    var isFPSLimited = false
    /**
     * This method return true in case if camera FPS is set to be displayed on panel surface.
     * Default value returned is false.
     *
     * @return True if camera FPS is set to be displayed on panel surface
     * @see .setFPSDisplayed
     */
    /**
     * This method is to control if camera FPS should be displayed on the webcam panel surface.
     *
     * @param displayed the value to control if camera FPS should be displayed
     */
    /**
     * Display FPS.
     */
    var isFPSDisplayed = false
    /**
     * This method will return true in case when panel is configured to display image size. The
     * string will be printed in the right bottom corner of the panel surface.
     *
     * @return True in case if panel is configured to display image size
     */
    /**
     * Configure panel to display camera image size to be displayed.
     *
     * @param imageSizeDisplayed if true the pixel dimensions are displayed over the image.
     */
    /**
     * Display image size.
     */
    var isImageSizeDisplayed = false
    /**
     * @return True is antialiasing is enabled, false otherwise
     */
    /**
     * Turn on/off antialiasing.
     *
     * @param antialiasing the true to enable, false to disable antialiasing
     */
    /**
     * Is antialiasing enabled (true by default).
     */
    var isAntialiasingEnabled = true
    /**
     * Return [Webcam] used by this panel.
     *
     * @return [Webcam]
     */
    /**
     * Webcam object used to fetch images.
     */
    val webcam: Webcam
    private val supplier: ImageSupplier

    /**
     * Repainter is used to fetch images from camera and force panel repaint when image is ready.
     */
    private val updater: ImageUpdater
    /**
     * @return [BufferedImage] displayed on [WebcamPanel]
     */
    /**
     * Image currently being displayed.
     */
    var image: BufferedImage? = null
        private set
    /**
     * Is webcam panel repainting starting.
     *
     * @return True if panel is starting
     */
    /**
     * Webcam is currently starting.
     */
    @Volatile
    var isStarting = false
        private set

    /**
     * Painting is paused.
     */
    @Volatile
    private var paused = false
    /**
     * Indicates whether the panel is in an error state
     *
     * @return true if the panel has an error present
     */
    /**
     * Is there any problem with webcam?
     */
    @Volatile
    var isErrored = false
        private set

    /**
     * Webcam has been started.
     */
    private val started = AtomicBoolean(false)
    /**
     * Get default painter used to draw panel.
     *
     * @return Default painter
     */
    /**
     * Default painter.
     */
    val defaultPainter: Painter = DefaultPainter()
    /**
     * Get painter used to draw image in webcam panel.
     *
     * @return Painter object
     */
    /**
     * Set new painter. Painter is a class which pains image visible when
     *
     * @param painter the painter object to be set
     */
    /**
     * Painter used to draw image in panel.
     *
     * @see .setPainter
     * @see .getPainter
     */
    var painter = defaultPainter

    /**
     * Preferred panel size.
     */
    private var defaultSize: Dimension? = null
    /**
     * Is displaying of some debug information enabled.
     *
     * @return True if debug information are enabled, false otherwise
     */
    /**
     * Display some debug information on image surface.
     *
     * @param displayDebugInfo the value to control debug information
     */
    /**
     * If debug info should be displayed.
     */
    var isDisplayDebugInfo = false
    /**
     * This method returns true if image mirroring is enabled. The default value is false.
     *
     * @return True if image is mirrored, false otherwise
     */
    /**
     * Decide whether or not the image from webcam painted on panel surface will be mirrored. The
     * image from camera itself is not modified.
     *
     * @param mirrored the parameter to control if image should be mirrored
     */
    /**
     * Is image mirrored.
     */
    var isMirrored = false
    /**
     * Creates new webcam panel which display image from camera in you your Swing application.
     *
     * @param webcam the webcam to be used to fetch images
     * @param start true if webcam shall be automatically started
     */
    /**
     * Creates webcam panel and automatically start webcam.
     *
     * @param webcam the webcam to be used to fetch images
     */
    @JvmOverloads
    constructor(webcam: Webcam, start: Boolean = true) : this(webcam, null, start) {
    }

    /**
     * Creates new webcam panel which display image from camera in you your Swing application. If
     * panel size argument is null, then image size will be used. If you would like to fill panel
     * area with image even if its size is different, then you can use
     * [WebcamPanel.setFillArea] method to configure this.
     *
     * @param webcam the webcam to be used to fetch images
     * @param size the size of panel
     * @param start true if webcam shall be automatically started
     * @see WebcamPanel.setFillArea
     */
    init {
        defaultSize = size
        this.webcam = webcam
        updater = ImageUpdater()
        this.supplier = supplier
        rb = WebcamUtils.loadRB(WebcamPanel::class.java)
        isDoubleBuffered = true
        addPropertyChangeListener("locale", this)
        if (size == null) {
            var r = webcam.viewSize
            preferredSize = r
        } else {
            preferredSize = size
        }
        if (start) {
            start()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (image == null) {
            painter.paintPanel(this, g as Graphics2D)
        } else {
            painter.paintImage(this, image, g as Graphics2D)
        }
    }

    /**
     * Open webcam and start rendering.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        webcam.addWebcamListener(this)
        LOG.debug("Starting panel rendering and trying to open attached webcam")
        updater.start()
        isStarting = true
        val worker: SwingWorker<Void, Void> = object : SwingWorker<Void, Void>() {
            @Throws(Exception::class)
            override fun doInBackground(): Void? {
                try {
                    if (!webcam.isOpen()) {
                        isErrored = !webcam.open()
                    }
                } catch (e: WebcamException) {
                    isErrored = true
                    throw e
                } finally {
                    isStarting = false
                    repaintPanel()
                }
                return null
            }
        }
        worker.execute()
    }

    /**
     * Stop rendering and close webcam.
     */
    fun stop() {
        if (!started.compareAndSet(true, false)) {
            return
        }
        webcam.removeWebcamListener(this)
        LOG.debug("Stopping panel rendering and closing attached webcam")
        try {
            updater.stop()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        image = null
        val worker: SwingWorker<Void?, Void?> = object : SwingWorker<Void?, Void?>() {
            @Throws(Exception::class)
            override fun doInBackground(): Void? {
                try {
                    if (webcam.isOpen()) {
                        isErrored = !webcam.close()
                    }
                } catch (e: WebcamException) {
                    isErrored = true
                    throw e
                } finally {
                    repaintPanel()
                }
                return null
            }
        }
        worker.execute()
    }

    /**
     * Repaint panel in Swing asynchronous manner.
     */
    private fun repaintPanel() {
        SwingUtilities.invokeLater(repaint)
    }

    /**
     * Pause rendering.
     */
    fun pause() {
        if (paused) {
            return
        }
        LOG.debug("Pausing panel rendering")
        paused = true
    }

    /**
     * Resume rendering.
     */
    fun resume() {
        if (!paused) {
            return
        }
        LOG.debug("Resuming panel rendering")
        paused = false
    }
    /**
     * Get rendering frequency in FPS (equivalent to Hz).
     *
     * @return Rendering frequency
     */
    /**
     * Set rendering frequency (in Hz or FPS). Minimum frequency is 0.016 (1 frame per minute) and
     * maximum is 25 (25 frames per second).
     *
     * @param fps the frequency
     */
    var fPSLimit: Double
        get() = frequency
        set(value) {
            var fps = value
            if (fps > MAX_FREQUENCY) {
                fps = MAX_FREQUENCY
            }
            if (fps < MIN_FREQUENCY) {
                fps = MIN_FREQUENCY
            }
            frequency = fps
        }

    /**
     * Is webcam panel repainting started.
     *
     * @return True if panel repainting has been started
     */
    fun isStarted(): Boolean {
        return started.get()
    }

    /**
     * This method will change the mode of panel area painting so the image will be resized and will
     * keep scale factor to fit into drawable panel bounds. When set to false, the mode will be
     * reset to [DrawMode.NONE] so image will be drawn as it is.
     *
     * @param fitArea the fit area mode enabled or disabled
     */
    @get:Deprecated("")
    @set:Deprecated("use {@link #setDrawMode(DrawMode drawMode)} instead.")
    var isFitArea: Boolean
        get() = drawMode == DrawMode.FIT
        set(fitArea) {
            drawMode = if (fitArea) DrawMode.FIT else DrawMode.NONE
        }
    /**
     * Get value of fill area setting. Image will be resized to fill panel area if true. If false
     * then image will be rendered as it was obtained from webcam instance.
     *
     * @return True if image is being resized, false otherwise
     */
    /**
     * Image will be resized to fill panel area if true. If false then image will be rendered as it
     * was obtained from webcam instance.
     *
     * @param fillArea shall image be resided to fill panel area
     */
    @get:Deprecated("use {@link #getDrawMode()} instead.")
    @set:Deprecated("use {@link #setDrawMode(DrawMode drawMode)} instead.")
    var isFillArea: Boolean
        get() = drawMode == DrawMode.FILL
        set(fillArea) {
            drawMode = if (fillArea) DrawMode.FILL else DrawMode.NONE
        }

    override fun propertyChange(evt: PropertyChangeEvent) {
        val lc = evt.newValue as Locale
        rb = WebcamUtils.loadRB(WebcamPanel::class.java)
    }

    override fun webcamOpen(we: WebcamEvent?) {

        // if default size has not been provided, then use the one from webcam
        // device (this will be current webcam resolution)
        if (defaultSize == null) {
            preferredSize = webcam.viewSize
        }
    }

    override fun webcamClosed(we: WebcamEvent?) {
        stop()
    }

    override fun webcamDisposed(we: WebcamEvent?) {
        stop()
    }

    override fun webcamImageObtained(we: WebcamEvent?) {
        // do nothing
    }

    companion object {
        /**
         * S/N used by Java to serialize beans.
         */
        private const val serialVersionUID = 1L

        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamPanel::class.java)

        /**
         * Minimum FPS frequency.
         */
        const val MIN_FREQUENCY = 0.016 // 1 frame per minute

        /**
         * Maximum FPS frequency.
         */
        private const val MAX_FREQUENCY = 50.0 // 50 frames per second

        /**
         * Thread factory used by execution service.
         */
        private val THREAD_FACTORY: ThreadFactory = PanelThreadFactory()
        val DEFAULT_IMAGE_RENDERING_HINTS: MutableMap<RenderingHints.Key?, Any?> = HashMap()

        init {
            DEFAULT_IMAGE_RENDERING_HINTS[RenderingHints.KEY_INTERPOLATION] =
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            DEFAULT_IMAGE_RENDERING_HINTS[RenderingHints.KEY_RENDERING] = RenderingHints.VALUE_RENDER_SPEED
            DEFAULT_IMAGE_RENDERING_HINTS[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        }
    }
}