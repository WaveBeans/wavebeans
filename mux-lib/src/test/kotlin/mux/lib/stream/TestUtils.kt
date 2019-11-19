package mux.lib.stream

import mux.lib.*
import mux.lib.io.StreamOutput
import mux.lib.io.Writer

fun FiniteSampleStream.toDevNull(
): StreamOutput<SampleArray, FiniteSampleStream> {
    return DevNullSampleStreamOutput(this, NoParams())
}

class DevNullSampleStreamOutput(
        val stream: FiniteSampleStream,
        params: NoParams
) : StreamOutput<SampleArray, FiniteSampleStream>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {

        val sampleIterator = stream.asSequence(sampleRate).iterator()

        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    sampleIterator.next()
                    true
                } else {
                    false
                }
            }

            override fun close() {}

        }
    }

    override val input: Bean<SampleArray, FiniteSampleStream>
        get() = stream

    override val parameters: BeanParams = params

}