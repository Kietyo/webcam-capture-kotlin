package com.github.sarxos.webcam

import com.github.sarxos.webcam.util.ImageUtils
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import javax.imageio.ImageIO

object WebcamUtils {
    fun capture(webcam: Webcam, file: File?) {
        if (!webcam.isOpen()) {
            webcam.open()
        }
        try {
            ImageIO.write(webcam.image, ImageUtils.FORMAT_JPG, file)
        } catch (e: IOException) {
            throw WebcamException(e)
        }
    }

    fun capture(webcam: Webcam, file: File?, format: String?) {
        if (!webcam.isOpen()) {
            webcam.open()
        }
        try {
            ImageIO.write(webcam.image, format, file)
        } catch (e: IOException) {
            throw WebcamException(e)
        }
    }

    fun capture(webcam: Webcam, filename: String) {
        var filename = filename
        if (!filename.endsWith(".jpg")) {
            filename = "$filename.jpg"
        }
        capture(webcam, File(filename))
    }

    fun capture(webcam: Webcam, filename: String, format: String) {
        var filenameCopy = filename
        val ext = "." + format.lowercase(Locale.getDefault())
        if (!filenameCopy.endsWith(ext)) {
            filenameCopy += ext
        }
        capture(webcam, File(filenameCopy), format)
    }

    fun getImageBytes(webcam: Webcam, format: String?): ByteArray {
        return ImageUtils.toByteArray(webcam.image, format)
    }

    /**
     * Capture image as BYteBuffer.
     *
     * @param webcam the webcam from which image should be obtained
     * @param format the file format
     * @return Byte buffer
     */
    fun getImageByteBuffer(webcam: Webcam, format: String?): ByteBuffer {
        return ByteBuffer.wrap(getImageBytes(webcam, format))
    }

    /**
     * Get resource bundle for specific class.
     *
     * @param clazz the class for which resource bundle should be found
     * @param locale the [Locale] object
     * @return Resource bundle
     */
    fun loadRB(clazz: Class<*>, locale: Locale?): ResourceBundle {
        val pkg = WebcamUtils::class.java.getPackage().name.replace("\\.".toRegex(), "/")
        return PropertyResourceBundle.getBundle(String.format("%s/i18n/%s", pkg, clazz.simpleName))
    }
}