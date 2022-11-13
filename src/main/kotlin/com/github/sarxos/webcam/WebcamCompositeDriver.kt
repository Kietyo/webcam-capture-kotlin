package com.github.sarxos.webcam

class WebcamCompositeDriver(vararg drivers: WebcamDriver) : WebcamDriver, WebcamDiscoverySupport {
    private val drivers: MutableList<WebcamDriver> = ArrayList()

    init {
        for (driver in drivers) {
            this.drivers.add(driver)
        }
    }

    fun add(driver: WebcamDriver) {
        drivers.add(driver)
    }

    fun getDrivers(): List<WebcamDriver> {
        return drivers
    }

    override val devices: List<WebcamDevice>
        get() {
            val all: MutableList<WebcamDevice> = ArrayList()
            for (driver in drivers) {
                all.addAll(driver.devices)
            }
            return all
        }
    override val isThreadSafe: Boolean
        get() {
            var safe = true
            for (driver in drivers) {
                safe = safe and driver.isThreadSafe
                if (!safe) {
                    break
                }
            }
            return safe
        }

    private var backingScanInterval: Long = -1L

    override var scanInterval: Long
        get() {
            return if (backingScanInterval <= 0) {
                WebcamDiscoverySupport.DEFAULT_SCAN_INTERVAL
            } else backingScanInterval
        }
        set(value) {
            backingScanInterval = value
        }

    override val isScanPossible: Boolean
        get() = true
}