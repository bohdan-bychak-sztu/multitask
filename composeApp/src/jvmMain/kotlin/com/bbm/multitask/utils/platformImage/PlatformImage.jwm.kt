package com.bbm.multitask.utils.platformImage

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual fun loadPlatformImage(path: String): PlatformImage {
    return JvmImage(ImageIO.read(File(path)))
}

class JvmImage(private val image: BufferedImage) : PlatformImage {
    override val width: Int = image.width
    override val height: Int = image.height

    override fun getPixel(x: Int, y: Int) = image.getRGB(x, y)
    override fun setPixel(x: Int, y: Int, color: Int) = image.setRGB(x, y, color)

    override fun save(outputPath: String) {
        ImageIO.write(image, "png", File(outputPath))
    }

    override fun getBytes(): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    override fun toImageBitmap(): ImageBitmap {
        return image.toComposeImageBitmap()
    }
}
