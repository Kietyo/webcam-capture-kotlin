package com.github.sarxos.webcam

/**
 * This interface should be implemented by all webcam drivers which would like to support webcam
 * devices discovery mechanism.
 *
 * @author Bartosz Firyn (SarXos)
 */
interface WebcamDiscoverySupport {
    /**
     * Get interval between next discovery scans. Time interval is given in milliseconds.
     *
     * @return Time interval between next scans
     */
    val scanInterval: Long

    /**
     * Check if scan is possible. In some cases, even if driver support devices discovery, there can
     * be a situation when due to various factors, scan cannot be executed (e.g. devices are busy,
     * network is unavailable, devices registry not responding, etc). In general this method should
     * return true.
     *
     * @return True if scan possible, false otherwise
     */
    val isScanPossible: Boolean

    companion object {
        /**
         * Default webcam discovery scan interval in milliseconds.
         */
        const val DEFAULT_SCAN_INTERVAL: Long = 2000
    }
}