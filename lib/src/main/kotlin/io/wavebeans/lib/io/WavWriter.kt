package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlin.reflect.KClass

class AnyWavWriter(
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

class AnyWavPartialWriter<A : Any>(
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
