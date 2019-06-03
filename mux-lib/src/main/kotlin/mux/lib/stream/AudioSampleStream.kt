package mux.lib.stream

import mux.lib.io.AudioInput
import java.util.concurrent.TimeUnit

class AudioSampleStream(
        val input: AudioInput
) : SampleStream {
    override fun info(namespace: String?): Map<String, String> {
        return input.info(namespace)
    }

    override fun samplesCount(): Int = input.size()

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream =
            AudioSampleStream(input.rangeProjection(start, end, timeUnit))
}