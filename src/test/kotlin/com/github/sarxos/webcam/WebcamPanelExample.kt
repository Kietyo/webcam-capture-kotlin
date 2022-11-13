package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamPanel.DrawMode
import java.awt.FlowLayout
import javax.swing.JFrame

object WebcamPanelExample {
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val window = JFrame("Raspberrypi Capture Example")
        window.isResizable = true
        window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        window.contentPane.layout = FlowLayout()
        val resolution = WebcamResolution.HQVGA.size
        for (webcam in Webcam.webcams) {
            webcam.setCustomViewSizess(resolution)
            webcam.viewSize = resolution
            webcam.open()
            val panel = WebcamPanel(webcam)
            panel.isDisplayDebugInfo = true
            panel.isFPSDisplayed = true
            panel.drawMode = DrawMode.FILL
            panel.isImageSizeDisplayed = true
            panel.preferredSize = resolution
            window.contentPane.add(panel)
        }
        window.pack()
        window.isVisible = true
    }
}