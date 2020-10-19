package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.SampleArray
import io.wavebeans.lib.SinglePartitionBean
import kotlin.reflect.KClass

class WavWriterFromSampleArray(
        stream: BeanStream<SampleArray>,
        val bitDepth: BitDepth,
        sampleRate: Float,
        val numberOfChannels: Int,
        writerDelegate: WriterDelegate<Unit>,
        outputClazz: KClass<*>
) : AbstractWriter<SampleArray>(stream, sampleRate, writerDelegate, outputClazz), SinglePartitionBean {

    private var dataSize: Int = 0

    override fun header(): ByteArray? {
        return WavHeader(bitDepth, sampleRate, numberOfChannels, dataSize).header()
    }

    override fun footer(): ByteArray? = null

    override fun serialize(element: SampleArray): ByteArray {
        val buf = ByteArray(bitDepth.bytesPerSample * element.size)
        dataSize += bitDepth.bytesPerSample
        element.forEachIndexed { i, e ->
            writeSampleAsLEBytes(buf, i * bitDepth.bytesPerSample, e, bitDepth)
        }

        return buf
    }

}