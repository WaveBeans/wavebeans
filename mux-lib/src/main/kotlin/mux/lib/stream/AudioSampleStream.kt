package mux.lib.stream

import mux.lib.io.AudioInput

class AudioSampleStream(val input: AudioInput, sampleRate: Float) : SampleStream(sampleRate) {
    override fun info(): Map<String, String> {
        return mapOf(
                "Sample rate" to "${sampleRate}Hz",
                "Length" to "${length() / 1000.0f}s"
        ) + input.info()
    }

    override fun samplesCount(): Int = input.size()

    override fun asSequence(): Sequence<Sample> = input.asSequence()

    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val s = Math.max(sampleStartIdx, 0)
        val e = Math.min(sampleEndIdx, input.size())

        return AudioSampleStream(input.subInput(s, e - s), sampleRate)
    }
}