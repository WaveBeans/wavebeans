package mux.lib.stream

class RatioDownSampledStream(
        private val sourceStream: SampleStream,
        private val ratio: Int,
        sourceStartIdx: Int? = null,
        sourceEndIdx: Int? = null

) : SampleStream(sourceStream.sampleRate / ratio) {

    override fun info(namespace: String?): Map<String, String> {
        val prefix = namespace?.let { "[$it] " } ?: ""
        return sourceStream.info("${prefix}Source") + mapOf(
                "${prefix}Sample rate" to "${sampleRate}Hz",
                "${prefix}Length" to "${length() / 1000.0f}s",
                "${prefix}Downsampling factor" to "$ratio"
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