package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamPanel.DrawMode
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() {
    val webcam = Webcam.getDefault() ?: error("No webcam found")

    val resolution = WebcamResolution.FHD.size
    webcam.setCustomViewSizess(resolution)
    webcam.viewSize = resolution
    webcam.open()

    SwingUtilities.invokeLater {
        val panel = WebcamPanel(webcam).apply {
            drawMode = DrawMode.FIT
            isFPSDisplayed = true
            isDisplayDebugInfo = true
            isImageSizeDisplayed = true
        }

        val window = JFrame("Webcam Demo").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            layout = BorderLayout()
            contentPane.add(panel, BorderLayout.CENTER)
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            webcam.close()
        })
    }
}
