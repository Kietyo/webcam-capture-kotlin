package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLoggerFactory

/**
 * Used internally.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamExceptionHandler private constructor() : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        val context: Any = LoggerFactory.getILoggerFactory()
        if (context is NOPLoggerFactory) {
            System.err.println(String.format("Exception in thread %s", t.name))
            e.printStackTrace()
        } else {
            LOG.error(String.format("Exception in thread %s", t.name), e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamExceptionHandler::class.java)
        val instance = WebcamExceptionHandler()
        @JvmStatic
		fun handle(e: Throwable) {
            instance.uncaughtException(Thread.currentThread(), e)
        }
    }
}