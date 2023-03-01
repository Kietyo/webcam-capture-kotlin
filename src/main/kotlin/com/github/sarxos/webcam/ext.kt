package com.github.sarxos.webcam

@JvmInline
value class NonNegativeTimeout(val value: Long) {
    init {
        require(value >= 0) {
            "Timeout cannot be negative ($value)"
        }
    }
    companion object {
        val MAX = NonNegativeTimeout(Long.MAX_VALUE)
    }
}