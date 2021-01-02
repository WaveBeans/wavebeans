package io.wavebeans.lib.io

import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.Sample
import io.wavebeans.lib.asUnsignedByte
import io.wavebeans.lib.sampleOf

class ByteArrayLittleEndianDecoder(val bitDepth: BitDepth) {

    fun sequence(buffer: ByteArray): Sequence<Sample> {
        var bufferPos = 0

        return object : Iterator<Sample> {
            override fun hasNext(): Boolean = bufferPos + bitDepth.bytesPerSample <= buffer.size

            override fun next(): Sample {
                val sample = buffer.decodeSampleLEBytes(bufferPos, bitDepth)
                bufferPos += bitDepth.bytesPerSample
                return sample
            }

        }.asSequence()
    }
}

fun ByteArray.decodeSampleLEBytes(at: Int, bitDepth: BitDepth): Sample {
    require(at < this.size && at >= 0) { "Index at=$at is outside byte array boundaries (size=${this.size}" }
    return when (bitDepth) {
        BitDepth.BIT_8 -> sampleOf(this[at].asUnsignedByte().toByte())
        BitDepth.BIT_16 -> sampleOf(
                ((this[at].toInt() and 0xFF) or
                        (this[at + 1].toInt() and 0xFF shl 8)
                        ).toShort()
        )
        BitDepth.BIT_24 -> sampleOf(
                ((this[at].toInt() and 0xFF) or
                        (this[at + 1].toInt() and 0xFF shl 8) or
                        (this[at + 2].toInt() and 0xFF shl 16)
                        ).let {
                            if (it and 0x800000 != 0) -(it.inv() and 0x7FFFFF)
                            else it and 0x7FFFFF
                        },
                as24bit = true
        )
        BitDepth.BIT_32 -> sampleOf(
                ((this[at].toInt() and 0xFF) or
                        (this[at + 1].toInt() and 0xFF shl 8) or
                        (this[at + 2].toInt() and 0xFF shl 16) or
                        (this[at + 3].toInt() and 0xFF shl 24)
                        )
        )
        BitDepth.BIT_64 -> sampleOf(
                ((this[at].toLong() and 0xFF) or
                        (this[at + 1].toLong() and 0xFF shl 8) or
                        (this[at + 2].toLong() and 0xFF shl 16) or
                        (this[at + 3].toLong() and 0xFF shl 24) or
                        (this[at + 4].toLong() and 0xFF shl 32) or
                        (this[at + 5].toLong() and 0xFF shl 40) or
                        (this[at + 6].toLong() and 0xFF shl 48) or
                        (this[at + 7].toLong() and 0xFF shl 46)
                        )
        )
    }
}