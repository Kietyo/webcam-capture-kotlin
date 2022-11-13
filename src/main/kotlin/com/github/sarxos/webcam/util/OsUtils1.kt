package com.github.sarxos.webcam.util

import java.util.*

/**
 * Just a simple enumeration with supported (not yet confirmed) operating
 * systems.
 *
 * @author Bartosz Firyn (sarxos)
 */
enum class OsUtils {
    /**
     * Microsoft Windows
     */
    WIN,

    /**
     * Linux or UNIX.
     */
    NIX,

    /**
     * Mac OS X
     */
    OSX;

    companion object {
//        private lateinit var os: OsUtils
//
//        /**
//         * Get operating system.
//         *
//         * @return OS
//         */
//        val oS: OsUtils
//            get() {
//                if (os == null) {
//                    val osp = System.getProperty("os.name").lowercase(Locale.getDefault())
//                    if (osp.indexOf("win") >= 0) {
//                        os = WIN
//                    } else if (osp.indexOf("mac") >= 0) {
//                        os = OSX
//                    } else if (osp.indexOf("nix") >= 0 || osp.indexOf("nux") >= 0) {
//                        os = NIX
//                    } else {
//                        throw RuntimeException("$osp is not supported")
//                    }
//                }
//                return os
//            }

        val oS = run {
            val osp = System.getProperty("os.name").lowercase(Locale.getDefault())
            return@run if (osp.indexOf("win") >= 0) {
                WIN
            } else if (osp.indexOf("mac") >= 0) {
                OSX
            } else if (osp.indexOf("nix") >= 0 || osp.indexOf("nux") >= 0) {
                NIX
            } else {
                throw RuntimeException("$osp is not supported")
            }
        }
    }
}