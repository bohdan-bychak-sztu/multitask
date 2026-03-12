package com.bbm.multitask.utils.platformImage

import androidx.compose.ui.graphics.ImageBitmap

interface PlatformImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
    fun setPixel(x: Int, y: Int, color: Int)
    fun save(outputPath: String)
    fun getBytes(): ByteArray
    fun toImageBitmap(): ImageBitmap
}

expect fun loadPlatformImage(path: String): PlatformImage