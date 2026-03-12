package com.bbm.multitask.utils.platformImage

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun loadPlatformImage(image: PlatformFile): PlatformImage {
    val bytes = image.readBytes()

    val uint8Array = Uint8Array(bytes.size)
    for (i in bytes.indices) {
        uint8Array[i] = bytes[i]
    }

    val blobData = JsArray<JsAny?>()
    blobData[0] = uint8Array

    val blob = Blob(blobData, BlobPropertyBag(type = "image/png"))
    val url = URL.createObjectURL(blob)

    return suspendCancellableCoroutine { continuation ->
        val img = document.createElement("img") as HTMLImageElement

        img.onload = {
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = img.width
            canvas.height = img.height
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
            ctx.drawImage(img, 0.0, 0.0)
            URL.revokeObjectURL(url)
            continuation.resume(WasmImage(canvas))
            null
        }

        img.onerror = { _, _, _, _, _ ->
            continuation.resumeWithException(Exception("Failed to load image"))
            null
        }
        img.src = url
    }
}

class WasmImage(private val canvas: HTMLCanvasElement) : PlatformImage {
    @OptIn(ExperimentalWasmJsInterop::class)
    private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    private var fullImageData: ImageData? = null

    private fun ensureDataLoaded(): ImageData {
        if (fullImageData == null) {
            fullImageData = ctx.getImageData(0.0, 0.0, width.toDouble(), height.toDouble())
        }
        return fullImageData!!
    }

    override val width: Int = canvas.width
    override val height: Int = canvas.height

    override fun getPixel(x: Int, y: Int): Int {
        val data = ensureDataLoaded().data
        val index = (y * width + x) * 4
        val r = data[index].toInt() and 0xFF
        val g = data[index + 1].toInt() and 0xFF
        val b = data[index + 2].toInt() and 0xFF
        val a = data[index + 3].toInt() and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun setPixel(x: Int, y: Int, color: Int) {
        val data = ensureDataLoaded().data
        val index = (y * width + x) * 4

        data[index] = ((color shr 16) and 0xFF).toByte()     // R
        data[index + 1] = ((color shr 8) and 0xFF).toByte()  // G
        data[index + 2] = (color and 0xFF).toByte()          // B
        data[index + 3] = ((color shr 24) and 0xFF).toByte() // A
    }

    fun finalizeChanges() {
        fullImageData?.let {
            ctx.putImageData(it, 0.0, 0.0)
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun save(outputPath: String) {
        val bytes = getBytes() // Отримуємо байти від Skia
        val uint8Array = Uint8Array(bytes.size)
        for (i in bytes.indices) uint8Array[i] = bytes[i]

        val blobData = JsArray<JsAny?>()
        blobData[0] = uint8Array
        val blob = Blob(blobData, BlobPropertyBag(type = "image/png"))
        val url = URL.createObjectURL(blob)

        val link = document.createElement("a") as HTMLAnchorElement
        link.setAttribute("download", outputPath)
        link.href = url
        link.click()

        URL.revokeObjectURL(url)
    }
    @OptIn(ExperimentalWasmJsInterop::class)
    override fun getBytes(): ByteArray {
        finalizeChanges()
        val dataUrl = canvas.toDataURL("image/png")
        val base64 = dataUrl.substringAfter(",")
        val binaryString = window.atob(base64)
        return ByteArray(binaryString.length) { binaryString[it].code.toByte() }
    }

    override fun toImageBitmap(): ImageBitmap {
        finalizeChanges()
        return SkiaImage.makeFromEncoded(getBytes()).toComposeImageBitmap()
    }
}