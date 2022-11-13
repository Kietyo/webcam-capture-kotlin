package com.github.sarxos.webcam.utilimport

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

/**
 * This class will save [BufferedImage] into a byte array and try to compress it a given size.
 *
 * @author Bartosz Firyn (sarxos)
 */
class AdaptiveSizeWriter(@field:Volatile private var size: Int) {
    private val baos = ByteArrayOutputStream()
    private var quality = 1f // 1f = 100% quality, at the beginning
    fun write(bi: BufferedImage): ByteArray {

        // loop and try to compress until compressed image bytes array is not longer than a given
        // maximum value, reduce quality by 25% in every step
        val m = size
        var s: Int
        var i = 0
        do {
            if (compress(bi, quality).also { s = it } > m) {
                quality *= 0.75.toFloat()
                if (i++ >= 20) {
                    break
                }
            }
        } while (s > m)
        return baos.toByteArray()
    }

    /**
     * Compress [BufferedImage] with a given quality into byte array.
     *
     * @param bi the [BufferedImage] to compres into byte array
     * @param quality the compressed image quality (1 = 100%, 0.5 = 50%, 0.1 = 10%, etc)
     * @return The size of compressed data (number of bytes)
     */
    private fun compress(bi: BufferedImage, quality: Float): Int {
        baos.reset()
        val params = JPEGImageWriteParam(null)
        params.compressionMode = ImageWriteParam.MODE_EXPLICIT
        params.compressionQuality = quality
        try {
            MemoryCacheImageOutputStream(baos).use { mcios ->
                val writer = ImageIO.getImageWritersByFormatName("jpg").next()
                writer.output = mcios
                writer.write(null, IIOImage(bi, null, null), params)
            }
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
        return baos.size()
    }

    fun getSize(): Int {
        return size
    }

    fun setSize(size: Int) {
        if (this.size != size) {
            this.size = size
            quality = AdaptiveSizeWriter.Companion.INITIAL_QUALITY
        }
    }

    companion object {
        private const val INITIAL_QUALITY = 1f
    }
}