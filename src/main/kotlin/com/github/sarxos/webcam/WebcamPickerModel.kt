package com.github.sarxos.webcam

import javax.swing.DefaultComboBoxModel

class WebcamPickerModel(webcams: List<Webcam>) : DefaultComboBoxModel<Webcam?>(webcams.toTypedArray()) {
    override fun getSelectedItem(): Webcam {
        return super.getSelectedItem() as Webcam
    }

    override fun setSelectedItem(webcam: Any) {
        require(webcam is Webcam) { "Selected object has to be an Webcam instance" }
        super.setSelectedItem(webcam)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}