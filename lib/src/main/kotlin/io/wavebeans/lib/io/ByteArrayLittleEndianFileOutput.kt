package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FiniteSampleStream
import java.net.URI

abstract class ByteArrayLEFileOutputWriter(
        uri: URI,
        stream: BeanStream<Sample>,
        val bitDepth: BitDepth,
        sampleRate: Float
) : FileWriter<Sample>(uri, stream, sampleRate), SinglePartitionBean {

    protected var dataSize = 0
        private set

    override fun serialize(element: Sample): ByteArray {
        val buf = ByteArray(bitDepth.bytesPerSample)
        dataSize += bitDepth.bytesPerSample

        if (bitDepth != BitDepth.BIT_8) {
            val bytesAsLong = when (bitDepth) {
                BitDepth.BIT_16 -> element.asShort().toLong()
                BitDepth.BIT_24 -> element.as24BitInt().toLong()
                BitDepth.BIT_32 -> element.asInt().toLong()
                BitDepth.BIT_64 -> element.asLong()
                BitDepth.BIT_8 -> throw UnsupportedOperationException("Unreachable branch")
            }

            (0 until bitDepth.bytesPerSample)
                    .map { j -> ((bytesAsLong shr j * 8) and 0xFF).toByte() }
                    .forEachIndexed { j: Int, v: Byte ->
                        buf[j] = (v.toInt() and 0xFF).toByte()
                    }
        } else {
            // 8 bit is kept as unsigned. https://en.wikipedia.org/wiki/WAV#Description
            // Follow this for explanation why it's done this way
            // https://en.wikipedia.org/wiki/Two's_complement#Converting_from_two.27s-complement_representation
            val asUByte = element.asByte().asUnsignedByte()
            buf[0] = asUByte.toByte()
        }

        return buf
    }

}