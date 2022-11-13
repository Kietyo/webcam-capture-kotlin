package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO

/**
 * This is very simple class which allows video from webcam to be exposed as MJPEG stream on a given
 * port. The mapping between webcam and port is one-to-one, which means that a single port need to
 * be allocated for every webcam you want to stream from.
 *
 * @author Bartoisz Firyn (sarxos)
 */
class WebcamStreamer(val port: Int, val webcam: Webcam?, fps: Double, start: Boolean) : ThreadFactory, WebcamListener {
    private inner class Acceptor : Runnable {
        override fun run() {
            try {
                ServerSocket(port, 50, InetAddress.getByName("0.0.0.0")).use { server ->
                    while (started.get()) {
                        executor.execute(Connection(server.accept()))
                    }
                }
            } catch (e: Exception) {
                LOG.error("Cannot accept socket connection", e)
            }
        }
    }

    private inner class Connection(private val socket: Socket?) : Runnable {

        override fun run() {
            LOG.info("New connection from {}", socket!!.remoteSocketAddress)
            val br: BufferedReader
            val bos: BufferedOutputStream
            val baos = ByteArrayOutputStream()
            try {
                br = BufferedReader(InputStreamReader(socket.getInputStream()))
                bos = BufferedOutputStream(socket.getOutputStream())
            } catch (e: IOException) {
                LOG.error("Fatal I/O exception when creating socket streams", e)
                try {
                    socket.close()
                } catch (e1: IOException) {
                    LOG.error("Canot close socket connection from " + socket.remoteSocketAddress, e1)
                }
                return
            }

            // consume whole input
            try {
                while (br.ready()) {
                    br.readLine()
                }
            } catch (e: IOException) {
                LOG.error("Error when reading input", e)
                return
            }

            // stream
            try {
                socket.soTimeout = 0
                socket.keepAlive = false
                socket.tcpNoDelay = true
                while (started.get()) {
                    val sb = StringBuilder()
                    sb.append("HTTP/1.0 200 OK").append(CRLF)
                    sb.append("Connection: close").append(CRLF)
                    sb.append("Cache-Control: no-cache").append(CRLF)
                    sb.append("Cache-Control: private").append(CRLF)
                    sb.append("Pragma: no-cache").append(CRLF)
                    sb.append("Content-type: multipart/x-mixed-replace; boundary=--").append(BOUNDARY).append(CRLF)
                    sb.append(CRLF)
                    bos.write(sb.toString().toByteArray())
                    do {
                        if (!webcam!!.isOpen() || socket.isInputShutdown || socket.isClosed) {
                            br.close()
                            bos.close()
                            return
                        }
                        baos.reset()
                        val now = System.currentTimeMillis()
                        if (now > last + delay) {
                            image = webcam.image
                        }
                        ImageIO.write(image, "JPG", baos)
                        sb.delete(0, sb.length)
                        sb.append("--").append(BOUNDARY).append(CRLF)
                        sb.append("Content-type: image/jpeg").append(CRLF)
                        sb.append("Content-Length: ").append(baos.size()).append(CRLF)
                        sb.append(CRLF)
                        try {
                            bos.write(sb.toString().toByteArray())
                            bos.write(baos.toByteArray())
                            bos.write(CRLF.toByteArray())
                            bos.flush()
                        } catch (e: SocketException) {
                            if (!socket.isConnected) {
                                LOG.debug("Connection to client has been lost")
                            }
                            if (socket.isClosed) {
                                LOG.debug("Connection to client is closed")
                            }
                            try {
                                br.close()
                                bos.close()
                            } catch (se: SocketException) {
                                LOG.debug("Exception when closing socket", se)
                            }
                            LOG.debug("Socket exception from " + socket.remoteSocketAddress, e)
                            return
                        }
                        Thread.sleep(delay)
                    } while (started.get())
                }
            } catch (e: Exception) {
                val message = e.message
                if (message != null) {
                    if (message.startsWith("Software caused connection abort")) {
                        LOG.info("User closed stream")
                        return
                    }
                    if (message.startsWith("Broken pipe")) {
                        LOG.info("User connection broken")
                        return
                    }
                }
                LOG.error("Error", e)
                try {
                    bos.write("HTTP/1.0 501 Internal Server Error\r\n\r\n\r\n".toByteArray())
                } catch (e1: IOException) {
                    LOG.error("Not ablte to write to output stream", e)
                }
            } finally {
                LOG.info("Closing connection from {}", socket.remoteSocketAddress)
                for (closeable in arrayOf(br, bos, baos)) {
                    try {
                        closeable.close()
                    } catch (e: IOException) {
                        LOG.debug("Cannot close socket", e)
                    }
                }
                try {
                    socket.close()
                } catch (e: IOException) {
                    LOG.debug("Cannot close socket", e)
                }
            }
        }
    }

    var fPS = 0.0
    private var number = 0
    private val last: Long = -1
    private var delay: Long = -1
    private var image: BufferedImage? = null
    private val executor = Executors.newCachedThreadPool(this)
    private val started = AtomicBoolean(false)

    init {
        requireNotNull(webcam) { "Webcam for streaming cannot be null" }
        fPS = fps
        delay = (1000 / fps).toLong()
        if (start) {
            start()
        }
    }

    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, String.format("streamer-thread-%s", number++))
        thread.uncaughtExceptionHandler = WebcamExceptionHandler.instance
        thread.isDaemon = true
        return thread
    }

    fun start() {
        if (started.compareAndSet(false, true)) {
            webcam!!.addWebcamListener(this)
            webcam.open()
            executor.execute(Acceptor())
        }
    }

    fun stop() {
        if (started.compareAndSet(true, false)) {
            executor.shutdown()
            webcam!!.removeWebcamListener(this)
            webcam.close()
        }
    }

    override fun webcamOpen(we: WebcamEvent?) {
        start()
    }

    override fun webcamClosed(we: WebcamEvent?) {
        stop()
    }

    override fun webcamDisposed(we: WebcamEvent?) {}
    override fun webcamImageObtained(we: WebcamEvent?) {}
    val isInitialized: Boolean
        get() = started.get()

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamStreamer::class.java)
        private const val BOUNDARY = "mjpegframe"
        private const val CRLF = "\r\n"
    }
}