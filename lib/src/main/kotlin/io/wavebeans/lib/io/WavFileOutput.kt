package io.wavebeans.lib.io

import io.wavebeans.lib.*
import java.net.URI


fun BeanStream<Sample>.toMono8bitWav(uri: String): StreamOutput<Sample> {
    return WavFileOutput(this, WavFileOutputParams(uri, BitDepth.BIT_8, 1))
}

fun BeanStream<Sample>.toMono16bitWav(uri: String): StreamOutput<Sample> {
    return WavFileOutput(this, WavFileOutputParams(uri, BitDepth.BIT_16, 1))
}

fun BeanStream<Sample>.toMono24bitWav(uri: String): StreamOutput<Sample> {
    return WavFileOutput(this, WavFileOutputParams(uri, BitDepth.BIT_24, 1))
}

fun BeanStream<Sample>.toMono32bitWav(uri: String): StreamOutput<Sample> {
    return WavFileOutput(this, WavFileOutputParams(uri, BitDepth.BIT_32, 1))
}


class WavOutputException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}

data class WavFileOutputParams(
        val uri: String,
        val bitDepth: BitDepth,
        val numberOfChannels: Int
) : BeanParams()

class WavFileOutput(
        val stream: BeanStream<Sample>,
        val params: WavFileOutputParams
) : StreamOutput<Sample>, SinglePartitionBean {

    override val input: Bean<Sample>
        get() = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer =
            WavWriter(stream, params.bitDepth, sampleRate, params.numberOfChannels, FileBufferedWriter(URI(params.uri)))

}

