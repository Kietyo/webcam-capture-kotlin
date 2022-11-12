package com.github.sarxos.webcam

import org.slf4j.LoggerFactory

/**
 * Shutdown hook to be executed when JVM exits gracefully. This class intention
 * is to be used internally only.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamShutdownHook constructor(
    /**
     * Webcam instance to be disposed / closed.
     */
    private val webcam: Webcam?) : Thread("shutdown-hook-" + ++number) {

    /**
     * Create new shutdown hook instance.
     *
     * @param webcam the webcam for which hook is intended
     */
    init {
        uncaughtExceptionHandler = WebcamExceptionHandler.instance
    }

    override fun run() {
        LOG.info("Automatic {} deallocation", webcam!!.name)
        webcam.dispose()
    }

    companion object {
        /**
         * Logger.
         */
        private val LOG = LoggerFactory.getLogger(WebcamShutdownHook::class.java)

        /**
         * Number of shutdown hook instance.
         */
        private var number = 0
    }
}