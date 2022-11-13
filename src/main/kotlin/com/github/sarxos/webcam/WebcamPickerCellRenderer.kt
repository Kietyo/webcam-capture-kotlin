package com.github.sarxos.webcam

import java.awt.Component
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class WebcamPickerCellRenderer : JLabel(), ListCellRenderer<Webcam> {
    init {
        isOpaque = true
        horizontalAlignment = LEFT
        verticalAlignment = CENTER
        icon = ICON
    }

    override fun getListCellRendererComponent(
        list: JList<out Webcam>,
        webcam: Webcam,
        i: Int,
        selected: Boolean,
        focused: Boolean
    ): Component {
        if (selected) {
            background = list.selectionBackground
            foreground = list.selectionForeground
        } else {
            background = list.background
            foreground = list.foreground
        }
        text = webcam.name
        font = list.font
        return this
    }

    companion object {
        private const val serialVersionUID = 1L
        private val ICON =
            ImageIcon(WebcamPickerCellRenderer::class.java.getResource("/com/github/sarxos/webcam/icons/camera-icon.png"))
    }
}