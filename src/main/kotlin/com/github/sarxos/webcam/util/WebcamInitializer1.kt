package com.github.sarxos.webcam.util

import com.github.sarxos.webcam.WebcamException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Atomic webcam initializer.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamInitializer(private val initializable: Initializable) {
    private val initialized = AtomicBoolean(false)
    private val latch = CountDownLatch(1)
    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            try {
                initializable.initialize()
            } catch (e: Exception) {
                throw WebcamException(e)
            } finally {
                latch.countDown()
            }
        } else {
            try {
                latch.await()
            } catch (e: InterruptedException) {
                return
            }
        }
    }

    fun teardown() {
        if (initialized.compareAndSet(true, false)) {
            try {
                initializable.teardown()
            } catch (e: Exception) {
                throw WebcamException(e)
            }
        }
    }

    fun isInitialized(): Boolean {
        return initialized.get()
    }
}