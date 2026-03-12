package com.bbm.multitask.utils.platformImage

import kotlin.math.*

class PvdProcessor(val image: PlatformImage) {

    private val ranges = listOf(
        0..7, 8..15, 16..31, 32..63, 64..127, 128..255
    )

    fun embedData(dataToHide: ByteArray) {
        val headerBits = StegoHeader(length = dataToHide.size).toBitArray()
        val dataIterator = BitIterator(dataToHide)
        var headerCursor = 0
        val totalHeaderBits = StegoHeader.HEADER_BITS_SIZE

        for (y in 0 until image.height) {
            for (x in 0 until image.width - 1 step 2) {
                if (headerCursor >= totalHeaderBits && !dataIterator.hasNext(1)) return

                val p1 = image.getPixel(x, y) and 0xFF
                val p2 = image.getPixel(x + 1, y) and 0xFF
                val d = abs(p1 - p2)

                val range = ranges.first { d in it }
                val n = floor(log2((range.last - range.first + 1).toDouble())).toInt()

                if (!isUsable(p1, p2, n, range)) continue

                var bitsToEmbed = 0
                var bitsProcessedInThisStep = 0

                while (bitsProcessedInThisStep < n) {
                    val spaceLeft = n - bitsProcessedInThisStep
                    if (headerCursor < totalHeaderBits) {
                        val take = minOf(spaceLeft, totalHeaderBits - headerCursor)
                        val chunk = extractBitsFromHeader(headerBits, headerCursor, take)
                        bitsToEmbed = (bitsToEmbed shl take) or chunk
                        headerCursor += take
                        bitsProcessedInThisStep += take
                    } else if (dataIterator.hasNext(1)) {
                        val take = spaceLeft // Беремо все, що залишилося в n
                        val chunk = dataIterator.getNextBits(take)
                        bitsToEmbed = (bitsToEmbed shl take) or chunk
                        bitsProcessedInThisStep += take
                    } else {
                        bitsToEmbed = bitsToEmbed shl spaceLeft
                        bitsProcessedInThisStep += spaceLeft
                    }
                }

                val dPrime = range.first + bitsToEmbed
                val m = dPrime - d
                val (p1New, p2New) = adjustPixels(p1, p2, m)

                image.setPixel(x, y, (image.getPixel(x, y) and -0x100) or (p1New and 0xFF))
                image.setPixel(x + 1, y, (image.getPixel(x + 1, y) and -0x100) or (p2New and 0xFF))
            }
        }
    }

    fun extractData(): ByteArray {
        val headerBitsCollector = mutableListOf<Int>()
        var dataCollector = BitCollector()
        var header: StegoHeader? = null

        for (y in 0 until image.height) {
            for (x in 0 until image.width - 1 step 2) {
                val p1 = image.getPixel(x, y) and 0xFF
                val p2 = image.getPixel(x + 1, y) and 0xFF
                val dPrime = abs(p1 - p2)

                val range = ranges.find { dPrime in it } ?: continue
                val n = floor(log2((range.last - range.first + 1).toDouble())).toInt()
                if (!isUsable(p1, p2, n, range)) continue

                var extractedBits = dPrime - range.first
                var bitsInPixel = n

                while (bitsInPixel > 0) {
                    if (header == null) {
                        val neededForHeader = StegoHeader.HEADER_BITS_SIZE - headerBitsCollector.size
                        val take = minOf(bitsInPixel, neededForHeader)

                        val shift = bitsInPixel - take
                        val chunk = (extractedBits shr shift) and ((1 shl take) - 1)

                        for (i in (take - 1) downTo 0) {
                            headerBitsCollector.add((chunk shr i) and 1)
                        }

                        if (headerBitsCollector.size >= StegoHeader.HEADER_BITS_SIZE) {
                            header = StegoHeader.parse(headerBitsCollector.toIntArray())
                        }

                        bitsInPixel -= take
                        extractedBits = extractedBits and ((1 shl bitsInPixel) - 1)
                    } else {
                        dataCollector.addBits(extractedBits, bitsInPixel)
                        if (dataCollector.sizeInBytes >= header.length) {
                            return dataCollector.toByteArray().sliceArray(0 until header.length)
                        }
                        bitsInPixel = 0
                    }
                }
            }
        }
        return dataCollector.toByteArray()
    }

    private fun adjustPixels(p1: Int, p2: Int, m: Int): Pair<Int, Int> {
        val mHalf = m / 2
        val mRemainder = m - mHalf

        var p1New: Int
        var p2New: Int

        if (p1 >= p2) {
            p1New = p1 + mRemainder
            p2New = p2 - mHalf
        } else {
            p1New = p1 - mRemainder
            p2New = p2 + mHalf
        }

        if (p1New < 0) { p2New -= p1New; p1New = 0 }
        else if (p1New > 255) { p2New -= (p1New - 255); p1New = 255 }

        if (p2New < 0) { p1New -= p2New; p2New = 0 }
        else if (p2New > 255) { p1New -= (p2New - 255); p2New = 255 }

        return p1New to p2New
    }

    private fun extractBitsFromHeader(bits: IntArray, start: Int, n: Int): Int {
        var res = 0
        for (i in 0 until n) {
            res = (res shl 1) or bits[start + i]
        }
        return res
    }

    fun isUsable(p1: Int, p2: Int, bitsToHide: Int, range: IntRange): Boolean {
        val dMax = range.last
        val d = abs(p1 - p2)
        val m = dMax - d
        val (testP1, testP2) = adjustPixels(p1, p2, m)
        return testP1 in 0..255 && testP2 in 0..255
    }
}

class BitIterator(private val data: ByteArray) {
    private var byteIndex = 0
    private var bitOffset = 0

    val remainingBits: Int
        get() = (data.size - byteIndex) * 8 - bitOffset

    fun hasNext(n: Int = 1): Boolean = remainingBits >= n

    fun getNextBits(n: Int): Int {
        var result = 0
        var bitsNeeded = n

        while (bitsNeeded > 0 && remainingBits > 0) {
            val bitsAvailableInByte = 8 - bitOffset
            val take = minOf(bitsNeeded, bitsAvailableInByte)

            val currentByte = data[byteIndex].toInt() and 0xFF
            val shift = bitsAvailableInByte - take
            val mask = ((1 shl take) - 1) shl shift
            val extracted = (currentByte and mask) shr shift

            result = (result shl take) or extracted

            bitsNeeded -= take
            bitOffset += take

            if (bitOffset == 8) {
                bitOffset = 0
                byteIndex++
            }
        }

        if (bitsNeeded > 0) {
            result = result shl bitsNeeded
        }

        return result
    }
}

class BitCollector {
    private val bytes = mutableListOf<Byte>()
    private var currentByte = 0
    private var bitCount = 0
    val sizeInBytes: Int
        get() = bytes.size

    fun addBits(value: Int, n: Int) {
        var bitsToAdd = n
        val valToProcess = value

        while (bitsToAdd > 0) {
            val spaceInByte = 8 - bitCount
            val take = minOf(bitsToAdd, spaceInByte)

            val shift = bitsToAdd - take
            val mask = ((1 shl take) - 1) shl shift
            val bits = (valToProcess and mask) shr shift

            currentByte = (currentByte shl take) or bits
            bitCount += take
            bitsToAdd -= take

            if (bitCount == 8) {
                bytes.add(currentByte.toByte())
                currentByte = 0
                bitCount = 0
            }
        }
    }

    fun toByteArray(): ByteArray = bytes.toByteArray()
}

data class StegoHeader(
    val version: Int = 1, // 4 біти
    val length: Int       // 32 біти
) {
    companion object {
        const val HEADER_BITS_SIZE = 4 + 32 // 36 біт

        fun parse(bits: IntArray): StegoHeader {
            fun bitsToInt(arr: IntArray, start: Int, len: Int): Int {
                var res = 0
                for (i in 0 until len) {
                    res = (res shl 1) or arr[start + i]
                }
                return res
            }
            return StegoHeader(
                version = bitsToInt(bits, 0, 4),
                length = bitsToInt(bits, 4, 32)
            )
        }
    }

    fun toBitArray(): IntArray {
        val result = IntArray(HEADER_BITS_SIZE)
        for (i in 0 until 4) result[i] = (version shr (3 - i)) and 1
        for (i in 0 until 32) result[i + 4] = (length shr (31 - i)) and 1
        return result
    }
}