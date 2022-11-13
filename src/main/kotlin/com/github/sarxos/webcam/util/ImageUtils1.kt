package com.github.sarxos.webcam.util

import com.github.sarxos.webcam.WebcamException
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

object ImageUtils {
    /**
     * Graphics Interchange Format.
     */
    const val FORMAT_GIF = "GIF"

    /**
     * Portable Network Graphic format.
     */
    const val FORMAT_PNG = "PNG"

    /**
     * Joint Photographic Experts Group format.
     */
    const val FORMAT_JPG = "JPG"

    /**
     * Bitmap image format.
     */
    const val FORMAT_BMP = "BMP"

    /**
     * Wireless Application Protocol Bitmap image format.
     */
    const val FORMAT_WBMP = "WBMP"

    /**
     * Convert [BufferedImage] to byte array.
     *
     * @param image the image to be converted
     * @param format the output image format
     * @return New array of bytes
     */
    fun toByteArray(image: BufferedImage?, format: String?): ByteArray {
        val bytes: ByteArray
        val baos = ByteArrayOutputStream()
        bytes = try {
            ImageIO.write(image, format, baos)
            baos.toByteArray()
        } catch (e: IOException) {
            throw WebcamException(e)
        } finally {
            try {
                baos.close()
            } catch (e: IOException) {
                throw WebcamException(e)
            }
        }
        return bytes
    }

    fun readFromResource(resource: String?): BufferedImage {
        var `is`: InputStream? = null
        return try {
            ImageIO.read(ImageUtils::class.java.classLoader.getResourceAsStream(resource).also { `is` = it })
        } catch (e: IOException) {
            throw IllegalStateException(e)
        } finally {
            if (`is` != null) {
                try {
                    `is`!!.close()
                } catch (e: IOException) {
                    throw IllegalStateException(e)
                }
            }
        }
    }

    fun createEmptyImage(source: BufferedImage): BufferedImage {
        return BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
    }

    /**
     * Clamp a value to the range 0..255
     */
    fun clamp(c: Int): Int {
        if (c < 0) {
            return 0
        }
        return if (c > 255) {
            255
        } else c
    }

    /**
     * Return image raster as bytes array.
     *
     * @param bi the [BufferedImage]
     * @return The raster data as byte array
     */
    fun imageToBytes(bi: BufferedImage): ByteArray {
        return (bi.data.dataBuffer as DataBufferByte).data
    }
}