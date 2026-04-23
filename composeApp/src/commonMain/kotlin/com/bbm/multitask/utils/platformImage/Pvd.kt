package com.bbm.multitask.utils.platformImage

import kotlin.math.*

enum class ColorChannel(val shift: Int) {
    RED(16), GREEN(8), BLUE(0)
}

class PvdProcessor(val image: PlatformImage) {
    private val ranges = listOf(
        0..7, 8..15, 16..31, 32..63, 64..127, 128..255
    )

    fun embedData(dataToHide: ByteArray, channels: List<ColorChannel> = listOf(ColorChannel.BLUE)) {
        val headerBits = StegoHeader(length = dataToHide.size).toBitArray()
        val dataIterator = BitIterator(dataToHide)
        var headerCursor = 0
        val totalHeaderBits = StegoHeader.HEADER_BITS_SIZE

        for (y in 0 until image.height) {
            for (x in 0 until image.width - 1 step 2) {
                for (channel in channels) {
                    if (headerCursor >= totalHeaderBits && !dataIterator.hasNext(1)) return

                    val p1Full = image.getPixel(x, y)
                    val p2Full = image.getPixel(x + 1, y)

                    val p1 = (p1Full shr channel.shift) and 0xFF
                    val p2 = (p2Full shr channel.shift) and 0xFF

                    val d = abs(p1 - p2)
                    val range = ranges.firstOrNull { d in it } ?: continue
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
                            val take = minOf(spaceLeft, dataIterator.remainingBits)
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

                    image.setPixel(x, y, updateChannel(p1Full, channel.shift, p1New))
                    image.setPixel(x + 1, y, updateChannel(p2Full, channel.shift, p2New))
                }
            }
        }
    }

    fun extractData(channels: List<ColorChannel> = listOf(ColorChannel.BLUE)): ByteArray {
        val headerBitsCollector = mutableListOf<Int>()
        val dataCollector = BitCollector()
        var header: StegoHeader? = null

        for (y in 0 until image.height) {
            for (x in 0 until image.width - 1 step 2) {
                for (channel in channels) {
                    val p1Full = image.getPixel(x, y)
                    val p2Full = image.getPixel(x + 1, y)

                    val p1 = (p1Full shr channel.shift) and 0xFF
                    val p2 = (p2Full shr channel.shift) and 0xFF

                    val dPrime = abs(p1 - p2)
                    val range = ranges.find { dPrime in it } ?: continue
                    val n = floor(log2((range.last - range.first + 1).toDouble())).toInt()

                    if (!isUsable(p1, p2, n, range)) continue

                    var extractedValue = dPrime - range.first
                    var bitsRemainingInPixel = n

                    while (bitsRemainingInPixel > 0) {
                        if (header == null) {
                            val neededForHeader = StegoHeader.HEADER_BITS_SIZE - headerBitsCollector.size
                            val take = minOf(bitsRemainingInPixel, neededForHeader)

                            val shift = bitsRemainingInPixel - take
                            val chunk = (extractedValue shr shift) and ((1 shl take) - 1)

                            for (i in (take - 1) downTo 0) {
                                headerBitsCollector.add((chunk shr i) and 1)
                            }

                            if (headerBitsCollector.size >= StegoHeader.HEADER_BITS_SIZE) {
                                header = StegoHeader.parse(headerBitsCollector.toIntArray())
                            }

                            bitsRemainingInPixel -= take
                            extractedValue = extractedValue and ((1 shl bitsRemainingInPixel) - 1)

                        } else {
                            dataCollector.addBits(extractedValue, bitsRemainingInPixel)

                            if (dataCollector.sizeInBytes >= header.length) {
                                return dataCollector.toByteArray().sliceArray(0 until header.length)
                            }
                            bitsRemainingInPixel = 0
                        }
                    }
                }
            }
        }
        return dataCollector.toByteArray()
    }

    private fun updateChannel(pixel: Int, shift: Int, newValue: Int): Int {
        val mask = (0xFF shl shift).inv()
        return (pixel and mask) or ((newValue and 0xFF) shl shift)
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

    fun calculatePvdForPair(p1: Int, p2: Int, charToEmbed: Char): String {
        val d = abs(p1 - p2)
        val range = ranges.firstOrNull { d in it }
            ?: return "Error: Could not find a suitable range for difference d=$d."

        val n = floor(log2((range.last - range.first + 1).toDouble())).toInt()

        if (n == 0) {
            return "Cannot embed any bits. This pixel pair can hide 0 bits."
        }

        val bitsToHide = minOf(n, 8)

        if (!isUsable(p1, p2, bitsToHide, range)) {
            return "This pixel pair is not usable. Embedding would cause overflow/underflow."
        }

        val charCode = charToEmbed.code
        val bitsToEmbed = charCode shr (8 - bitsToHide)
        val dPrime = range.first + bitsToEmbed
        val m = dPrime - d
        println("Original pixels: P1=$p1, P2=$p2\n" +
                "Difference d=$d falls in range ${range.first}..${range.last}, allowing n=$n bits.\n" +
                "Embedding character '${charToEmbed}' (code $charCode) requires hiding $bitsToHide bits.\n" +
                "Calculated d'=$dPrime, m=$m.")
        val (p1New, p2New) = adjustPixels(p1, p2, m)
        println("Adjusted pixels: P1'=$p1New, P2'=$p2New")

        val charBinary = charCode.toString(2).padStart(8, '0')
        val hiddenBits = charBinary.substring(0, bitsToHide)
        val remainingBits = charBinary.substring(bitsToHide)
        val binaryString = "Binary: `$hiddenBits`$remainingBits"

        return "New pixels: P1'=$p1New, P2'=$p2New\n" +
                "Embedded $bitsToHide of 8 bits.\n" +
                binaryString
    }

    fun extractPvdForPair(p1: Int, p2: Int): String {
        val dPrime = abs(p1 - p2)
        val range = ranges.firstOrNull { dPrime in it }
            ?: return "Error: Could not find a suitable range for difference d'=$dPrime."

        val n = floor(log2((range.last - range.first + 1).toDouble())).toInt()

        if (n == 0) {
            return "No bits are hidden in this pixel pair."
        }

        val extractedValue = dPrime - range.first
        val binaryString = extractedValue.toString(2).padStart(n, '0')

        return "Extracted bits: $binaryString\n" +
                "Value: $extractedValue ($n bits)"
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