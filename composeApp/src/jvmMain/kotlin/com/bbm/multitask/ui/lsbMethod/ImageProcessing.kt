package com.bbm.multitask.ui.lsbMethod

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import java.io.File
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.imageio.ImageIO
import kotlin.random.Random

interface PlatformImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
    fun setPixel(x: Int, y: Int, color: Int)
    fun save(outputPath: String)
    fun getBytes(): ByteArray
    fun toImageBitmap(): ImageBitmap
}

fun encodeImage(imagePath: String, message: String, bpc: Int = 1, secretKey: String? = null): PlatformImage {
    val image = loadPlatformImage(imagePath)
    val encryptedMessage = if (secretKey != null)
        Crypto.encrypt(message, secretKey)
    else
        message
    val data = encryptedMessage.toByteArray(Charsets.UTF_8)

    val header = StegoHeader(length = data.size)
    val headerBits = header.toBinaryString()
    val dataBits = data.joinToString("") { it.toUByte().toInt().toBinaryString(8) }

    var bitIndex = 0
    val totalBitsToHide = headerBits.length + dataBits.length

    val totalPixels = image.width * image.height
    val pixelIndices = IntArray(totalPixels) { it }

    if (secretKey != null) {
        val seed = secretKey.hashCode().toLong()
        pixelIndices.shuffle(Random(seed))
    }

    for (index in pixelIndices) {
        if (bitIndex >= totalBitsToHide) break

        val x = index % image.width
        val y = index / image.width

        var argb = image.getPixel(x, y)
        val a = (argb shr 24) and 0xFF
        val channels = mutableListOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

        for (i in channels.indices) {
            if (bitIndex < totalBitsToHide) {
                val currentBpc = if (bitIndex < StegoHeader.SIZE_BITS) 1 else bpc

                val remainingInHeader = StegoHeader.SIZE_BITS - bitIndex
                val bitsToTake = if (bitIndex < StegoHeader.SIZE_BITS) {
                    minOf(1, remainingInHeader)
                } else {
                    minOf(bpc, totalBitsToHide - bitIndex)
                }

                val source = if (bitIndex < StegoHeader.SIZE_BITS) headerBits else dataBits
                val offset = if (bitIndex < StegoHeader.SIZE_BITS) bitIndex else bitIndex - StegoHeader.SIZE_BITS

                val chunk = source.substring(offset, offset + bitsToTake).padEnd(currentBpc, '0')

                val mask = (0xFF shl currentBpc) and 0xFF
                channels[i] = (channels[i] and mask) or chunk.toInt(2)
                bitIndex += bitsToTake
            }
        }
        val newColor = (a shl 24) or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
        image.setPixel(x, y, newColor)

    }
    return image
}

fun Int.toBinaryString(length: Int): String =
    Integer.toBinaryString(this).padStart(length, '0').takeLast(length)

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

fun loadPlatformImage(path: String): PlatformImage {
    return JvmImage(ImageIO.read(File(path)))
}

fun decodeImage(imagePath: String, bpc: Int = 1, secretKey: String? = null): String {
    val image = loadPlatformImage(imagePath)
    val bitBuffer = StringBuilder()

    var messageLength: Int? = null
    val resultBytes = mutableListOf<Byte>()
    var totalBitsRead = 0

    val totalPixels = image.width * image.height
    val pixelIndices = IntArray(totalPixels) { it }

    if (secretKey != null) {
        val seed = secretKey.hashCode().toLong()
        pixelIndices.shuffle(Random(seed))
    }

    for (index in pixelIndices) {
        val x = index % image.width
        val y = index / image.width
        val argb = image.getPixel(x, y)
        val channels = listOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

        for (colorValue in channels) {
            if (messageLength == null) {
                bitBuffer.append((colorValue and 1).toString())

                if (bitBuffer.length == StegoHeader.SIZE_BITS) {
                    val magic = bitBuffer.substring(0, 8).toInt(2).toByte()
                    if (magic != '/'.code.toByte()) return "Error: No first byte found!"

                    messageLength = bitBuffer.substring(12, 44).toInt(2)
                    bitBuffer.setLength(0)
                }
            } else if (resultBytes.size < messageLength) {
                val extracted = (colorValue and ((1 shl bpc) - 1)).toBinaryString(bpc)
                bitBuffer.append(extracted)

                while (bitBuffer.length >= 8 && resultBytes.size < messageLength) {
                    val byteStr = bitBuffer.substring(0, 8)
                    resultBytes.add(byteStr.toInt(2).toByte())
                    bitBuffer.delete(0, 8)
                }
            }

            if (messageLength != null && resultBytes.size >= messageLength) {
                if (secretKey != null) {
                    val decrypted = Crypto.decrypt(String(resultBytes.toByteArray(), Charsets.UTF_8), secretKey)
                    return decrypted
                } else
                    return String(resultBytes.toByteArray(), Charsets.UTF_8)
            }
        }
    }
    return "Message not found or incomplete."
}

data class StegoHeader(
    val beginning: Byte = '/'.code.toByte(), // 8 біт
    val version: Int = 1,                // 4 біти
    val length: Int                      // 32 біти
) {
    companion object {
        const val SIZE_BITS = 8 + 4 + 32 // 44 біти
    }

    fun toBinaryString(): String {
        return beginning.toUByte().toInt().toBinaryString(8) +
                version.toBinaryString(4) +
                length.toBinaryString(32)
    }
}

object Crypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val iv = IvParameterSpec(ByteArray(16))

    private fun generateKey(password: String?): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password?.toByteArray())
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(text: String, password: String?): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, generateKey(password), iv)
        return Base64.getEncoder().encodeToString(cipher.doFinal(text.toByteArray()))
    }

    fun decrypt(encryptedText: String, password: String?): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, generateKey(password), iv)
        return String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)))
    }
}