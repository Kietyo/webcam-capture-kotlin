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

import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ColorModel

/**
 * A convenience class which implements those methods of BufferedImageOp which are rarely changed.
 */
abstract class JHFilter : BufferedImageOp {
    override fun createCompatibleDestImage(src: BufferedImage, dstCM: ColorModel?): BufferedImage {
        var dstCMCopy: ColorModel? = dstCM
        if (dstCMCopy == null) {
            dstCMCopy = src.colorModel
        }
        return BufferedImage(
            dstCMCopy,
            dstCMCopy!!.createCompatibleWritableRaster(src.width, src.height),
            dstCMCopy.isAlphaPremultiplied,
            null
        )
    }

    override fun getBounds2D(src: BufferedImage): Rectangle2D {
        return Rectangle(0, 0, src.width, src.height)
    }

    override fun getPoint2D(srcPt: Point2D, dstPt: Point2D?): Point2D {
        var dstPtCopy: Point2D? = dstPt
        if (dstPtCopy == null) {
            dstPtCopy = Point2D.Double()
        }
        dstPtCopy.setLocation(srcPt.x, srcPt.y)
        return dstPtCopy
    }

    override fun getRenderingHints(): RenderingHints? {
        return null
    }

    /**
     * A convenience method for getting ARGB pixels from an image. This tries to avoid the
     * performance penalty of BufferedImage.getRGB unmanaging the image.
     *
     * @param image a BufferedImage object
     * @param x the left edge of the pixel block
     * @param y the right edge of the pixel block
     * @param width the width of the pixel array
     * @param height the height of the pixel array
     * @param pixels the array to hold the returned pixels. May be null.
     * @return the pixels
     * @see .setRGB
     */
    fun getRGB(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, pixels: IntArray?): IntArray {
        val type = image.type
        return if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            image.raster.getDataElements(x, y, width, height, pixels) as IntArray
        } else image.getRGB(x, y, width, height, pixels, 0, width)
    }

    /**
     * A convenience method for setting ARGB pixels in an image. This tries to avoid the performance
     * penalty of BufferedImage.setRGB unmanaging the image.
     *
     * @param image a BufferedImage object
     * @param x the left edge of the pixel block
     * @param y the right edge of the pixel block
     * @param width the width of the pixel array
     * @param height the height of the pixel array
     * @param pixels the array of pixels to set
     * @see .getRGB
     */
    fun setRGB(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, pixels: IntArray?) {
        val type = image.type
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            image.raster.setDataElements(x, y, width, height, pixels)
        } else {
            image.setRGB(x, y, width, height, pixels, 0, width)
        }
    }
}