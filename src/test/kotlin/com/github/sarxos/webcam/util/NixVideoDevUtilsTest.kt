package com.github.sarxos.webcam.util

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Dan Rollo
 * Date: 3/8/14
 * Time: 10:44 PM
 */
class NixVideoDevUtilsTest {
    /**
     * Accept method was failing with exception: String index out of range: 5
     * This occurs on opensuse 11 where video device files do not all have a suffix. The files are created like so:
     * $ ls -l /dev/video*
     * /dev/video -&gt; video0
     * /dev/video0
     *
     * In this case, the link name 'video' is less that 6 characters long, so the filter statement:
     * Character.isDigit(name.charAt(5))
     * causes the exception.
     *
     * Fix is to also check for length before checking for isDigit().
     */
    @Test
    fun testAcceptHandlesShortVideoDeviceFilename() {
        val videoDeviceFilenameFilter = NixVideoDevUtils()
        assertFalse(videoDeviceFilenameFilter.accept(File("/dev"), "video"))
        assertTrue(videoDeviceFilenameFilter.accept(File("/dev"), "video0"))
    }
}