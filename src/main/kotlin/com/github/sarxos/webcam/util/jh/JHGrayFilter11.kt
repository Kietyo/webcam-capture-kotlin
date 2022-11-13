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

class JHGrayFilter : JHFilter() {
    protected var canFilterIndexColorModel = true
    override fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage {
        var dstCopy = dst
        val width = src.width
        val height = src.height
        val type = src.type
        val srcRaster = src.raster
        if (dstCopy == null) {
            dstCopy = createCompatibleDestImage(src, null)
        }
        val dstRaster = dstCopy.raster
        val inPixels = IntArray(width)
        for (y in 0 until height) {
            if (type == BufferedImage.TYPE_INT_ARGB) {
                srcRaster.getDataElements(0, y, width, 1, inPixels)
                for (x in 0 until width) {
                    inPixels[x] = filterRGB(inPixels[x])
                }
                dstRaster.setDataElements(0, y, width, 1, inPixels)
            } else {
                src.getRGB(0, y, width, 1, inPixels, 0, width)
                for (x in 0 until width) {
                    inPixels[x] = filterRGB(inPixels[x])
                }
                dstCopy.setRGB(0, y, width, 1, inPixels, 0, width)
            }
        }
        return dstCopy
    }

    private fun filterRGB(rgb: Int): Int {
        var newRgb = rgb
        val a = newRgb and -0x1000000
        val r = newRgb shr 16 and 0xff
        val g = newRgb shr 8 and 0xff
        val b = newRgb and 0xff
        newRgb = r * 77 + g * 151 + b * 28 shr 8 // NTSC luma
        return a or (newRgb shl 16) or (newRgb shl 8) or newRgb
    }
}