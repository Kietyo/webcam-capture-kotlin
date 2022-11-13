package com.github.sarxos.webcam

import javax.swing.JComboBox

class WebcamPicker @JvmOverloads constructor(webcams: List<Webcam> = Webcam.webcams) :
    JComboBox<Webcam>(WebcamPickerModel(webcams)) {
    init {
        setRenderer(RENDERER)
    }

    val selectedWebcam: Webcam
        get() = selectedItem as Webcam

    companion object {
        private const val serialVersionUID = 1L
        private val RENDERER = WebcamPickerCellRenderer()
    }
}