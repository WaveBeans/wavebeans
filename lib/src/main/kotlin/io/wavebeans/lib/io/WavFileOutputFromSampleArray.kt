package io.wavebeans.lib.io

import io.wavebeans.lib.*
import java.net.URI


fun BeanStream<SampleArray>.toMono8bitWav(uri: String): StreamOutput<SampleArray> {
    return WavFileOutputFromSampleArray(this, WavFileOutputParamsFromSampleArray(uri, BitDepth.BIT_8, 1))
}

fun BeanStream<SampleArray>.toMono16bitWav(uri: String): StreamOutput<SampleArray> {
    return WavFileOutputFromSampleArray(this, WavFileOutputParamsFromSampleArray(uri, BitDepth.BIT_16, 1))
}

fun BeanStream<SampleArray>.toMono24bitWav(uri: String): StreamOutput<SampleArray> {
    return WavFileOutputFromSampleArray(this, WavFileOutputParamsFromSampleArray(uri, BitDepth.BIT_24, 1))
}

fun BeanStream<SampleArray>.toMono32bitWav(uri: String): StreamOutput<SampleArray> {
    return WavFileOutputFromSampleArray(this, WavFileOutputParamsFromSampleArray(uri, BitDepth.BIT_32, 1))
}


data class WavFileOutputParamsFromSampleArray(
        val uri: String,
        val bitDepth: BitDepth,
        val numberOfChannels: Int
) : BeanParams()

class WavFileOutputFromSampleArray(
        val stream: BeanStream<SampleArray>,
        val params: WavFileOutputParamsFromSampleArray
) : StreamOutput<SampleArray>, SinglePartitionBean {

    override val input: Bean<SampleArray>
        get() = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer =
            WavWriterFromSampleArray(stream, params.bitDepth, sampleRate, params.numberOfChannels, FileWriterDelegate(URI(params.uri)))

}

