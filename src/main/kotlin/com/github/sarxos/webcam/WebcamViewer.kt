package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * Just a simple webcam viewer.
 *
 * @author Bartosz Firyn (SarXos)
 */
class WebcamViewer : JFrame(), Runnable, WebcamListener, WindowListener, Thread.UncaughtExceptionHandler, ItemListener {
    private lateinit var webcam: Webcam
    private var panel: WebcamPanel? = null
    private var picker: WebcamPicker? = null

    init {
        SwingUtilities.invokeLater(this)
    }

    override fun run() {
        title = "Webcam Capture Viewer"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        addWindowListener(this)
        picker = WebcamPicker()
        picker!!.addItemListener(this)
        webcam = picker!!.selectedWebcam
        webcam.viewSize = WebcamResolution.VGA.size
        webcam.addWebcamListener(this@WebcamViewer)
        panel = WebcamPanel(webcam, false)
        panel!!.isFPSDisplayed = true
        add(picker, BorderLayout.NORTH)
        add(panel, BorderLayout.CENTER)
        pack()
        isVisible = true
        val t: Thread = object : Thread() {
            override fun run() {
                panel!!.start()
            }
        }
        t.name = "webcam-viewer-starter"
        t.isDaemon = true
        t.uncaughtExceptionHandler = this
        t.start()
    }

    override fun webcamOpen(we: WebcamEvent?) {
        LOG.info("Webcam open")
    }

    override fun webcamClosed(we: WebcamEvent?) {
        LOG.info("Webcam closed")
    }

    override fun webcamDisposed(we: WebcamEvent?) {
        LOG.info("Webcam disposed")
    }

    override fun webcamImageObtained(we: WebcamEvent?) {
        // do nothing
    }

    override fun windowActivated(e: WindowEvent) {}
    override fun windowClosed(e: WindowEvent) {
        webcam!!.close()
    }

    override fun windowClosing(e: WindowEvent) {}
    override fun windowOpened(e: WindowEvent) {}
    override fun windowDeactivated(e: WindowEvent) {}
    override fun windowDeiconified(e: WindowEvent) {
        LOG.info("Webcam viewer resumed")
        panel!!.resume()
    }

    override fun windowIconified(e: WindowEvent) {
        LOG.info("Webcam viewer paused")
        panel!!.pause()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        e.printStackTrace()
        LOG.error(String.format("Exception in thread %s", t.name), e)
    }

    override fun itemStateChanged(e: ItemEvent) {
        if (e.item === webcam) {
            return
        }
        val tmp = panel
        remove(panel)
        webcam.removeWebcamListener(this)
        webcam = e.item as Webcam
        webcam.viewSize = WebcamResolution.VGA.size
        webcam.addWebcamListener(this)
        println("selected " + webcam!!.name)
        panel = WebcamPanel(webcam, false)
        add(panel, BorderLayout.CENTER)
        val t: Thread = object : Thread() {
            override fun run() {
                tmp!!.stop()
                panel!!.start()
            }
        }
        t.isDaemon = true
        t.uncaughtExceptionHandler = this
        t.start()
    }

    companion object {
        private const val serialVersionUID = 1L
        private val LOG = LoggerFactory.getLogger(WebcamViewer::class.java)
    }
}