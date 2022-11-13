package com.github.sarxos.webcam

import com.github.sarxos.webcam.WebcamProcessor.ProcessorThread

abstract class WebcamTask(threadSafe: Boolean, val device: WebcamDevice?) {
    private var doSync = !threadSafe
    private var processor: WebcamProcessor = WebcamProcessor.instance
    var throwable: Throwable? = null

    constructor(driver: WebcamDriver, device: WebcamDevice?) : this(driver.isThreadSafe, device) {}
    constructor(device: WebcamDevice) : this(false, device) {}

    /**
     * Process task by processor thread.
     *
     * @throws InterruptedException when thread has been interrupted
     */
    @Throws(InterruptedException::class)
    fun process() {
        val alreadyInSync = Thread.currentThread() is ProcessorThread
        if (alreadyInSync) {
            handle()
        } else {
            if (doSync) {
                processor.process(this)
            } else {
                handle()
            }
        }
    }

    abstract fun handle()
}