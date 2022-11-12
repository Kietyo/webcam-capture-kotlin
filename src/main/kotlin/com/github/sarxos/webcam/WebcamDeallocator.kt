package com.github.sarxos.webcam

import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream

/**
 * Deallocator which goal is to release all devices resources when SIGTERM
 * signal is detected.
 *
 * @author Bartosz Firyn (SarXos)
 */
internal open class WebcamDeallocator
/**
 * This constructor is used internally to create new deallocator for the
 * given devices array.
 *
 * @param devices the devices to be stored in deallocator
 */ private constructor(private val webcams: Array<Webcam>) {
    fun deallocate() {
        for (w in webcams) {
            try {
                w.dispose()
            } catch (t: Throwable) {
                caugh(t)
            }
        }
    }

    private fun caugh(t: Throwable) {
        val f = File(String.format("webcam-capture-hs-%s", System.currentTimeMillis()))
        var ps: PrintStream? = null
        try {
            t.printStackTrace(PrintStream(f).also { ps = it })
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            if (ps != null) {
                ps!!.close()
            }
        }
    }

    companion object {
        private val HANDLER = WebcamSignalHandler()

        /**
         * Store devices to be deallocated when TERM signal has been received.
         *
         * @param webcams the webcams array to be stored in deallocator
         */
        fun store(webcams: Array<Webcam>) {
            if (HANDLER.get() == null) {
                HANDLER.set(WebcamDeallocator(webcams))
            } else {
                throw IllegalStateException("Deallocator is already set!")
            }
        }

        fun unstore() {
            HANDLER.reset()
        }
    }
}