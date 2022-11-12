package com.github.sarxos.webcam

import java.awt.image.BufferedImage

interface WebcamImageTransformer {
    fun transform(image: BufferedImage?): BufferedImage?
}