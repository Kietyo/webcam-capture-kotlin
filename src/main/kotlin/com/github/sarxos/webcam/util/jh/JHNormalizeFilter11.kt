package com.github.sarxos.webcam.util.jh

import com.github.sarxos.webcam.util.jhimport.JHFilter
import com.github.sarxos.webcam.util.ImageUtils
import java.awt.image.BufferedImage

class JHNormalizeFilter : JHFilter() {
    override fun filter(src: BufferedImage, dest: BufferedImage): BufferedImage {
        val w = src.width
        val h = src.height
        var c: Int
        var a: Int
        var r: Int
        var g: Int
        var b: Int
        var i: Int
        var max = 1
        for (x in 0 until w) {
            for (y in 0 until h) {
                c = src.getRGB(x, y)
                a = ImageUtils.clamp(c shr 24 and 0xff)
                r = ImageUtils.clamp(c shr 16 and 0xff)
                g = ImageUtils.clamp(c shr 8 and 0xff)
                b = ImageUtils.clamp(c and 0xff)
                i = a shl 24 or (r shl 16) or (g shl 8) or b
                if (i > max) {
                    max = i
                }
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                c = src.getRGB(x, y)
                i = c * 256 / max
                dest.setRGB(x, y, i)
            }
        }
        return dest
    }
}