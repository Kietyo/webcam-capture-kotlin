/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.github.sarxos.webcam.util.jhimport

import java.awt.image.BufferedImage

/**
 * A filter which performs a box blur on an image. The horizontal and vertical blurs can be
 * specified separately and a number of iterations can be given which allows an approximation to
 * Gaussian blur.
 */
class JHBlurFilter : JHFilter {
    /**
     * Get the horizontal size of the blur.
     *
     * @return the radius of the blur in the horizontal direction
     * @see .setHRadius
     */
    /**
     * Set the horizontal size of the blur. Minimum hRadius value is 0.
     *
     * @param hRadius the radius of the blur in the horizontal direction
     * @see .getHRadius
     */
    var hRadius = 0f
    /**
     * Get the vertical size of the blur.
     *
     * @return the radius of the blur in the vertical direction
     * @see .setVRadius
     */
    /**
     * Set the vertical size of the blur. Minimal vRadius value is 0.
     *
     * @param vRadius the radius of the blur in the vertical direction
     * @see .getVRadius
     */
    var vRadius = 0f
    /**
     * Get the number of iterations the blur is performed.
     *
     * @return the number of iterations
     * @see .setIterations
     */
    /**
     * Set the number of iterations the blur is performed. Minimum value is 0.
     *
     * @param iterations the number of iterations
     * @see .getIterations
     */
    var iterations = 1
    /**
     * Get whether to premultiply the alpha channel.
     *
     * @return true to premultiply the alpha
     * @see .setPremultiplyAlpha
     */
    /**
     * Set whether to premultiply the alpha channel.
     *
     * @param premultiplyAlpha true to premultiply the alpha
     * @see .getPremultiplyAlpha
     */
    var premultiplyAlpha = true

    /**
     * Construct a default BoxBlurFilter.
     */
    constructor() {}

    /**
     * Construct a BoxBlurFilter.
     *
     * @param hRadius the horizontal radius of blur
     * @param vRadius the vertical radius of blur
     * @param iterations the number of time to iterate the blur
     */
    constructor(hRadius: Float, vRadius: Float, iterations: Int) {
        this.hRadius = hRadius
        this.vRadius = vRadius
        this.iterations = iterations
    }

    override fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage {
        var dstCopy = dst
        val width = src.width
        val height = src.height
        if (dstCopy == null) {
            dstCopy = createCompatibleDestImage(src, null)
        }
        val inPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        getRGB(src, 0, 0, width, height, inPixels)
        if (premultiplyAlpha) {
            JHBlurFilter.Companion.premultiply(inPixels, 0, inPixels.size)
        }
        for (i in 0 until iterations) {
            JHBlurFilter.Companion.blur(inPixels, outPixels, width, height, hRadius)
            JHBlurFilter.Companion.blur(outPixels, inPixels, height, width, vRadius)
        }
        JHBlurFilter.Companion.blurFractional(inPixels, outPixels, width, height, hRadius)
        JHBlurFilter.Companion.blurFractional(outPixels, inPixels, height, width, vRadius)
        if (premultiplyAlpha) {
            JHBlurFilter.Companion.unpremultiply(inPixels, 0, inPixels.size)
        }
        setRGB(dstCopy, 0, 0, width, height, inPixels)
        return dstCopy
    }
    /**
     * Get the size of the blur.
     *
     * @return the radius of the blur in the horizontal direction
     * @see .setRadius
     */
    /**
     * Set both the horizontal and vertical sizes of the blur. Minimum value is 0.
     *
     * @param radius the radius of the blur in both directions
     * @see .getRadius
     */
    var radius: Float
        get() = hRadius
        set(radius) {
            vRadius = radius
            hRadius = vRadius
        }

    override fun toString(): String {
        return "Blur/Box Blur..."
    }

    companion object {
        /**
         * Blur and transpose a block of ARGB pixels.
         *
         * @param in the input pixels
         * @param out the output pixels
         * @param width the width of the pixel array
         * @param height the height of the pixel array
         * @param radius the radius of blur
         */
        fun blur(`in`: IntArray, out: IntArray, width: Int, height: Int, radius: Float) {
            val widthMinus1 = width - 1
            val r = radius.toInt()
            val tableSize = 2 * r + 1
            val divide = IntArray(256 * tableSize)
            for (i in 0 until 256 * tableSize) {
                divide[i] = i / tableSize
            }
            var inIndex = 0
            for (y in 0 until height) {
                var outIndex = y
                var ta = 0
                var tr = 0
                var tg = 0
                var tb = 0
                for (i in -r..r) {
                    val rgb = `in`[inIndex + JHBlurFilter.Companion.clamp(i, 0, width - 1)]
                    ta += rgb shr 24 and 0xff
                    tr += rgb shr 16 and 0xff
                    tg += rgb shr 8 and 0xff
                    tb += rgb and 0xff
                }
                for (x in 0 until width) {
                    out[outIndex] = divide[ta] shl 24 or (divide[tr] shl 16) or (divide[tg] shl 8) or divide[tb]
                    var i1 = x + r + 1
                    if (i1 > widthMinus1) {
                        i1 = widthMinus1
                    }
                    var i2 = x - r
                    if (i2 < 0) {
                        i2 = 0
                    }
                    val rgb1 = `in`[inIndex + i1]
                    val rgb2 = `in`[inIndex + i2]
                    ta += (rgb1 shr 24 and 0xff) - (rgb2 shr 24 and 0xff)
                    tr += (rgb1 and 0xff0000) - (rgb2 and 0xff0000) shr 16
                    tg += (rgb1 and 0xff00) - (rgb2 and 0xff00) shr 8
                    tb += (rgb1 and 0xff) - (rgb2 and 0xff)
                    outIndex += height
                }
                inIndex += width
            }
        }

        fun blurFractional(`in`: IntArray, out: IntArray, width: Int, height: Int, radius: Float) {
            var radius = radius
            radius -= radius.toInt().toFloat()
            val f = 1.0f / (1 + 2 * radius)
            var inIndex = 0
            for (y in 0 until height) {
                var outIndex = y
                out[outIndex] = `in`[0]
                outIndex += height
                for (x in 1 until width - 1) {
                    val i = inIndex + x
                    val rgb1 = `in`[i - 1]
                    val rgb2 = `in`[i]
                    val rgb3 = `in`[i + 1]
                    var a1 = rgb1 shr 24 and 0xff
                    var r1 = rgb1 shr 16 and 0xff
                    var g1 = rgb1 shr 8 and 0xff
                    var b1 = rgb1 and 0xff
                    val a2 = rgb2 shr 24 and 0xff
                    val r2 = rgb2 shr 16 and 0xff
                    val g2 = rgb2 shr 8 and 0xff
                    val b2 = rgb2 and 0xff
                    val a3 = rgb3 shr 24 and 0xff
                    val r3 = rgb3 shr 16 and 0xff
                    val g3 = rgb3 shr 8 and 0xff
                    val b3 = rgb3 and 0xff
                    a1 = a2 + ((a1 + a3) * radius).toInt()
                    r1 = r2 + ((r1 + r3) * radius).toInt()
                    g1 = g2 + ((g1 + g3) * radius).toInt()
                    b1 = b2 + ((b1 + b3) * radius).toInt()
                    a1 *= f.toInt()
                    r1 *= f.toInt()
                    g1 *= f.toInt()
                    b1 *= f.toInt()
                    out[outIndex] = a1 shl 24 or (r1 shl 16) or (g1 shl 8) or b1
                    outIndex += height
                }
                out[outIndex] = `in`[width - 1]
                inIndex += width
            }
        }

        /**
         * Premultiply a block of pixels
         *
         * @param p pixels
         * @param offset the offset
         * @param length the length
         */
        fun premultiply(p: IntArray, offset: Int, length: Int) {
            var length = length
            length += offset
            for (i in offset until length) {
                val rgb = p[i]
                val a = rgb shr 24 and 0xff
                var r = rgb shr 16 and 0xff
                var g = rgb shr 8 and 0xff
                var b = rgb and 0xff
                val f = a * (1.0f / 255.0f)
                r *= f.toInt()
                g *= f.toInt()
                b *= f.toInt()
                p[i] = a shl 24 or (r shl 16) or (g shl 8) or b
            }
        }

        /**
         * Premultiply a block of pixels
         *
         * @param p the pixels
         * @param offset the offset
         * @param length the length
         */
        fun unpremultiply(p: IntArray, offset: Int, length: Int) {
            var length = length
            length += offset
            for (i in offset until length) {
                val rgb = p[i]
                val a = rgb shr 24 and 0xff
                var r = rgb shr 16 and 0xff
                var g = rgb shr 8 and 0xff
                var b = rgb and 0xff
                if (a != 0 && a != 255) {
                    val f = 255.0f / a
                    r *= f.toInt()
                    g *= f.toInt()
                    b *= f.toInt()
                    if (r > 255) {
                        r = 255
                    }
                    if (g > 255) {
                        g = 255
                    }
                    if (b > 255) {
                        b = 255
                    }
                    p[i] = a shl 24 or (r shl 16) or (g shl 8) or b
                }
            }
        }

        /**
         * Clamp a value to an interval.
         *
         * @param a the lower clamp threshold
         * @param b the upper clamp threshold
         * @param x the input parameter
         * @return the clamped value
         */
        fun clamp(x: Int, a: Int, b: Int): Int {
            return if (x < a) a else if (x > b) b else x
        }
    }
}