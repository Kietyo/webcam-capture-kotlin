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
package com.github.sarxos.webcam.util.jh

import com.github.sarxos.webcam.util.jhimport.JHFilter
import java.awt.image.BufferedImage

/**
 * A filter which flips images or rotates by multiples of 90 degrees.
 */
class JHFlipFilter @JvmOverloads constructor(operation: Int = FLIP_90CW) : JHFilter() {
    /**
     * Get the filter operation.
     *
     * @return the filter operation
     * @see .setOperation
     */
    /**
     * Set the filter operation.
     *
     * @param operation the filter operation
     * @see .getOperation
     */
    var operation = 0
    /**
     * Construct a FlipFilter.
     *
     * @param operation the filter operation
     */
    /**
     * Construct a FlipFilter which flips horizontally and vertically.
     */
    init {
        this.operation = operation
    }

    override fun filter(
        src: BufferedImage,
        dst: BufferedImage
    ): BufferedImage {
        var dstCopy: BufferedImage = dst
        val width = src.width
        val height = src.height
        val inPixels = getRGB(src, 0, 0, width, height, null)
        var newW = width
        var newH = height
        when (operation) {
            FLIP_90CW -> {
                newW = height
                newH = width
            }

            FLIP_90CCW -> {
                newW = height
                newH = width
            }

            FLIP_180 -> {}
        }
        val newPixels = IntArray(newW * newH)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val index = row * width + col
                var newRow = row
                var newCol = col
                when (operation) {
                    FLIP_90CW -> {
                        newRow = col
                        newCol = height - row - 1
                    }

                    FLIP_90CCW -> {
                        newRow = width - col - 1
                        newCol = row
                    }

                    FLIP_180 -> {
                        newRow = height - row - 1
                        newCol = width - col - 1
                    }
                }
                val newIndex = newRow * newW + newCol
                newPixels[newIndex] = inPixels[index]
            }
        }
        if (dstCopy == null) {
            val dstCM = src.colorModel
            dstCopy = BufferedImage(
                dstCM,
                dstCM.createCompatibleWritableRaster(newW, newH),
                dstCM.isAlphaPremultiplied,
                null
            )
        }
        setRGB(dstCopy, 0, 0, newW, newH, newPixels)
        return dstCopy
    }

    override fun toString(): String {
        when (operation) {
            FLIP_90CW -> return "Rotate 90"
            FLIP_90CCW -> return "Rotate -90"
            FLIP_180 -> return "Rotate 180"
        }
        return "Flip"
    }

    companion object {
        /**
         * Rotate the image 90 degrees clockwise.
         */
        const val FLIP_90CW = 4

        /**
         * Rotate the image 90 degrees counter-clockwise.
         */
        const val FLIP_90CCW = 5

        /**
         * Rotate the image 180 degrees.
         */
        const val FLIP_180 = 6
    }
}