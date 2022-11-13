package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import sun.misc.Signal
import sun.misc.SignalHandler

/**
 * Primitive signal handler. This class is using undocumented classes from
 * sun.misc.* and therefore should be used with caution.
 *
 * @author Bartosz Firyn (SarXos)
 */
internal class WebcamSignalHandler : SignalHandler {
    private var deallocator: WebcamDeallocator? = null
    private var handler: SignalHandler? = null

    init {
        handler = Signal.handle(Signal("TERM"), this)
    }

    override fun handle(signal: Signal) {
        LOG.warn("Detected signal {} {}, calling deallocator", signal.name, signal.number)

        // do nothing on "signal default" or "signal ignore"
        if (handler === SignalHandler.SIG_DFL || handler === SignalHandler.SIG_IGN) {
            return
        }
        try {
            deallocator!!.deallocate()
        } finally {
            handler!!.handle(signal)
        }
    }

    fun set(deallocator: WebcamDeallocator?) {
        this.deallocator = deallocator
    }

    fun get(): WebcamDeallocator? {
        return deallocator
    }

    fun reset() {
        deallocator = null
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebcamSignalHandler::class.java)
    }
}