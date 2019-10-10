package mux.lib.io

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.io.InputStream
import java.net.URI

abstract class ByteArrayLittleEndianFileOutput(
        uri: URI,
        val finiteSampleStream: FiniteSampleStream,
        val bitDepth: BitDepth
) : FileStreamOutput<SampleArray, FiniteSampleStream>(finiteSampleStream, uri) {

    private var sampleRate: Float? = null

    protected fun sampleRate(): Float = sampleRate ?: throw IllegalStateException("Sample rate is not yet initialized")

    override fun serialize(offset: Long, sampleRate: Float, samples: List<SampleArray>): ByteArray {
//        this.sampleRate = sampleRate
//
//        val buf = ByteArray(samples.size * bitDepth.bytesPerSample)
//
//        var samplesRead = 0
//        samples.forEach { sample ->
//            if (bitDepth != BitDepth.BIT_8) {
//                val bytesAsLong = when (bitDepth) {
//                    BitDepth.BIT_16 -> sample.asShort().toLong()
//                    BitDepth.BIT_24 -> sample.as24BitInt().toLong()
//                    BitDepth.BIT_32 -> sample.asInt().toLong()
//                    BitDepth.BIT_64 -> sample.asLong()
//                    BitDepth.BIT_8 -> throw UnsupportedOperationException("Unreachable branch")
//                }
//
//                (0 until bitDepth.bytesPerSample)
//                        .map { i ->
//                            ((bytesAsLong shr i * 8) and 0xFF).toByte()
//                        }
//                        .forEachIndexed { i: Int, v: Byte ->
//                            buf[samplesRead * bitDepth.bytesPerSample + i] = (v.toInt() and 0xFF).toByte()
//                        }
//            } else {
//                // 8 bit is kept as unsigned. https://en.wikipedia.org/wiki/WAV#Description
//                // Follow this for explanation why it's done this way
//                // https://en.wikipedia.org/wiki/Two's_complement#Converting_from_two.27s-complement_representation
//                val asUByte = sample.asByte().asUnsignedByte()
//                buf[samplesRead * bitDepth.bytesPerSample] = asUByte.toByte()
//            }
//            samplesRead++
//        }
//
//        return buf
        TODO()
    }
}