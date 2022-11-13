package com.github.sarxos.webcam.ds.dummy

import com.github.sarxos.webcam.WebcamDevice
import com.github.sarxos.webcam.WebcamDiscoverySupport
import com.github.sarxos.webcam.WebcamDriver
import java.util.*
import kotlin.collections.ArrayList


class WebcamDummyDriver(private val count: Int) : WebcamDriver, WebcamDiscoverySupport {
    override val scanInterval: Long
        get() = 10000
    override val isScanPossible: Boolean
        get() = true
    override val devices: List<WebcamDevice>
        get() {
            val devices: MutableList<WebcamDevice> = ArrayList()
            for (i in 0 until count) {
                devices.add(WebcamDummyDevice(i))
            }
            return Collections.unmodifiableList(devices)
        }
    override val isThreadSafe: Boolean
        get() = false
}