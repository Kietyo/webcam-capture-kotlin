package com.github.sarxos.webcam

open class WebcamException : RuntimeException {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    constructor(cause: Throwable?) : super(cause) {}

    companion object {
        private const val serialVersionUID = 4305046981807594375L
    }
}