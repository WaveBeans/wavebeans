package mux.lib.stream

import mux.lib.io.AudioInput

class AudioSampleStream(
        val input: AudioInput
) : SampleStream {
    override fun info(namespace: String?): Map<String, String> {
        return input.info(namespace)
    }

    override fun samplesCount(): Int = input.size()

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val s = Math.max(sampleStartIdx, 0)
        val e = Math.min(sampleEndIdx, input.size())

        return AudioSampleStream(input.subInput(s, e - s))
    }
}