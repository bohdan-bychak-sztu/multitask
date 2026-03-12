package com.bbm.multitask.utils.platformImage

import androidx.compose.ui.graphics.ImageBitmap
import io.github.vinceglb.filekit.PlatformFile

interface PlatformImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
    fun setPixel(x: Int, y: Int, color: Int)
    fun save(outputPath: String)
    fun getBytes(): ByteArray
    fun toImageBitmap(): ImageBitmap
}

expect suspend fun loadPlatformImage(image: PlatformFile): PlatformImage