package io.wavebeans.lib.io

import io.wavebeans.lib.*

class WavWriter(
        stream: BeanStream<Sample>,
        val bitDepth: BitDepth,
        sampleRate: Float,
        val numberOfChannels: Int,
        writerDelegate: WriterDelegate
) : AbstractWriter<Sample>(stream, sampleRate, writerDelegate), SinglePartitionBean {

    private var dataSize: Int = 0

    override fun header(): ByteArray? {
        return WavHeader(bitDepth, sampleRate, numberOfChannels, dataSize).header()
    }

    override fun footer(): ByteArray? = null

    override fun serialize(element: Sample): ByteArray {
        val buf = ByteArray(bitDepth.bytesPerSample)
        dataSize += bitDepth.bytesPerSample

        writeSampleAsLEBytes(buf, 0, element, bitDepth)

        return buf
    }

}
