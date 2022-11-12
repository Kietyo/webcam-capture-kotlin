package com.github.sarxos.webcam.ds.test

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDriver
import java.util.*

class DummyDriver(override val devices: List<WebcamDevice> = DEVICES) : WebcamDriver {
    init {
        if (instance == null) {
            instance = this
        }
    }

    override val isThreadSafe: Boolean
        get() = false

    companion object {
        val DEVICES: List<WebcamDevice> = listOf(
            DummyDevice(),
            DummyDevice(),
            DummyDevice(),
            DummyDevice()
        )
        var instance: DummyDriver? = null
            private set
    }
}