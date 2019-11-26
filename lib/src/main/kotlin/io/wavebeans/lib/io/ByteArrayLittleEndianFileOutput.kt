package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FiniteSampleStream
import java.net.URI

abstract class ByteArrayLEFileOutputWriter(
        uri: URI,
        stream: FiniteSampleStream,
        val bitDepth: BitDepth,
        sampleRate: Float
) : FileWriter<SampleArray, FiniteSampleStream>(uri, stream, sampleRate), SinglePartitionBean {

    protected var dataSize = 0
        private set

    override fun serialize(element: SampleArray): ByteArray {
        val buf = ByteArray(element.size * bitDepth.bytesPerSample)
        dataSize += buf.size

        for (i in 0 until element.size) {
            val sample = element[i]
            if (bitDepth != BitDepth.BIT_8) {
                val bytesAsLong = when (bitDepth) {
                    BitDepth.BIT_16 -> sample.asShort().toLong()
                    BitDepth.BIT_24 -> sample.as24BitInt().toLong()
                    BitDepth.BIT_32 -> sample.asInt().toLong()
                    BitDepth.BIT_64 -> sample.asLong()
                    BitDepth.BIT_8 -> throw UnsupportedOperationException("Unreachable branch")
                }

                (0 until bitDepth.bytesPerSample)
                        .map { j -> ((bytesAsLong shr j * 8) and 0xFF).toByte() }
                        .forEachIndexed { j: Int, v: Byte ->
                            buf[i * bitDepth.bytesPerSample + j] = (v.toInt() and 0xFF).toByte()
                        }
            } else {
                // 8 bit is kept as unsigned. https://en.wikipedia.org/wiki/WAV#Description
                // Follow this for explanation why it's done this way
                // https://en.wikipedia.org/wiki/Two's_complement#Converting_from_two.27s-complement_representation
                val asUByte = sample.asByte().asUnsignedByte()
                buf[i * bitDepth.bytesPerSample] = asUByte.toByte()
            }
        }

        return buf
    }

}