package com.github.sarxos.webcam

class WebcamLockException : WebcamException {
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    constructor(message: String?) : super(message) {}
    constructor(cause: Throwable?) : super(cause) {}

    companion object {
        private const val serialVersionUID = 1L
    }
}