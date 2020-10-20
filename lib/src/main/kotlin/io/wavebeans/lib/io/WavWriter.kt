package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlin.reflect.KClass

class WavWriter(
        stream: BeanStream<Any>,
        val bitDepth: BitDepth,
        sampleRate: Float,
        val numberOfChannels: Int,
        writerDelegate: WriterDelegate<in Unit>,
        outputClazz: KClass<*>
) : AbstractWriter<Any>(stream, sampleRate, writerDelegate, outputClazz), SinglePartitionBean {

    private var dataSize: Int = 0

    override fun header(): ByteArray? = WavHeader(bitDepth, sampleRate, numberOfChannels, dataSize).header()

    override fun footer(): ByteArray? = null

    override fun serialize(element: Any): ByteArray = serializeWav(element, bitDepth) { dataSize += it }
}

class WavPartialWriter<A : Any>(
        stream: BeanStream<Managed<OutputSignal, A, Any>>,
        val bitDepth: BitDepth,
        sampleRate: Float,
        val numberOfChannels: Int,
        writerDelegate: WriterDelegate<A>,
        outputClazz: KClass<*>
) : AbstractPartialWriter<Any, A>(stream, sampleRate, writerDelegate, outputClazz), SinglePartitionBean {

    private var dataSize: Int = 0

    override fun header(): ByteArray? {
        val header = WavHeader(bitDepth, sampleRate, numberOfChannels, dataSize).header()
        dataSize = 0
        return header
    }

    override fun footer(): ByteArray? = null

    override fun serialize(element: Any): ByteArray = serializeWav(element, bitDepth) { dataSize += it }

}

private fun serializeWav(element: Any, bitDepth: BitDepth, dataSizeInc: (Int) -> Unit): ByteArray {
    return when (element) {
        is Sample -> {
            val buf = ByteArray(bitDepth.bytesPerSample)
            dataSizeInc(bitDepth.bytesPerSample)
            writeSampleAsLEBytes(buf, 0, element, bitDepth)
            buf
        }
        is SampleArray -> {
            val buf = ByteArray(bitDepth.bytesPerSample * element.size)
            dataSizeInc(bitDepth.bytesPerSample * element.size)
            for (i in element.indices) {
                writeSampleAsLEBytes(buf, i * bitDepth.bytesPerSample, element[i], bitDepth)
            }
            buf
        }
        else -> throw UnsupportedOperationException("${element::class} is unsupported")
    }
}

fun writeSampleAsLEBytes(buf: ByteArray, at: Int, element: Sample, bitDepth: BitDepth) {
    require(buf.size - at >= bitDepth.bytesPerSample) { "At least ${bitDepth.bytesPerSample} required but ${buf.size - at} available" }
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
                    buf[at + j] = (v.toInt() and 0xFF).toByte()
                }
    } else {
        // 8 bit is kept as unsigned. https://en.wikipedia.org/wiki/WAV#Description
        // Follow this for explanation why it's done this way
        // https://en.wikipedia.org/wiki/Two's_complement#Converting_from_two.27s-complement_representation
        val asUByte = element.asByte().asUnsignedByte()
        buf[at] = asUByte.toByte()
    }

}