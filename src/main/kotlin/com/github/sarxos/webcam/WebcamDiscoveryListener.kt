package com.github.sarxos.webcam

interface WebcamDiscoveryListener {
    fun webcamFound(event: WebcamDiscoveryEvent?)
    fun webcamGone(event: WebcamDiscoveryEvent?)
}