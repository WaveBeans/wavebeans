package mux.lib.stream

class RatioDownSampledStream(
        private val sourceStream: SampleStream,
        private val ratio: Int,
        sourceStartIdx: Int? = null,
        sourceEndIdx: Int? = null

) : SampleStream(sourceStream.sampleRate / ratio) {

    override fun info(): Map<String, String> {
        return sourceStream.info().map { "[Source] " + it.key to it.value }.toMap() + mapOf(
                "Sample rate" to "${sampleRate}Hz",
                "Length" to "${length() / 1000.0f}s",
                "Downsampling factor" to "$ratio"
        )
    }

    init {
        if (sourceStartIdx != null && sourceEndIdx ?: Int.MAX_VALUE <= sourceStartIdx)
            throw IllegalArgumentException("sourceStartIdx[$sourceStartIdx] should be less then sourceEndIdx[$sourceEndIdx]")
    }

    private val stream = if (sourceStartIdx != null) {
        sourceStream.rangeProjection(sourceStartIdx, sourceEndIdx ?: sourceStream.samplesCount())
    } else {
        sourceStream
    }

    override fun samplesCount(): Int = stream.samplesCount() / ratio

    override fun asSequence(): Sequence<Sample> {
        return stream.asSequence()
                .filterIndexed { index, _ -> index % ratio == 0 }

    }


    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val sourceStartIdx = sampleStartIdx * ratio
        val sourceEndIdx = sampleEndIdx * ratio

        return RatioDownSampledStream(sourceStream, ratio, sourceStartIdx, sourceEndIdx)
    }
}