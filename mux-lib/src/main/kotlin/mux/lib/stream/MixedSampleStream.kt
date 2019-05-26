package mux.lib.stream

class MixedSampleStream(
        val sourceStream: SampleStream,
        val mixInStream: SampleStream,
        val mixInPosition: Int,
        val startIndex: Int = 0,
        val endIndex: Int? = null
) : SampleStream {

    init {
        if (mixInPosition < 0) throw SampleStreamException("Position can't be negative")
        if (endIndex != null && startIndex >= endIndex) throw SampleStreamException("End sample index should be greater than start sample index")
        val length = Math.max(mixInStream.samplesCount() + mixInPosition, sourceStream.samplesCount())
        if (startIndex < 0 || startIndex >= length) throw SampleStreamException("Start sample index is out of range")
        if (endIndex != null && (endIndex < 0 || endIndex >= length)) throw SampleStreamException("End sample index is out of range")
    }

    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        return MixedSampleStream(sourceStream, mixInStream, mixInPosition, sampleStartIdx, sampleEndIdx)
    }

    override fun samplesCount(): Int {
        val initialLength = Math.max(mixInStream.samplesCount() + mixInPosition, sourceStream.samplesCount())
        return (endIndex?.let { it + 1 } ?: initialLength) - startIndex
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        // note: streams should be aligned by length as zip() uses the shortest one
        val alignedSourceStream = sourceStream.asSequence(sampleRate)
                // extend the stream to fit all samples if it's shorter
                .plus((0 until samplesCount() - sourceStream.samplesCount()).asSequence().map { ZeroSample })
        val alignedMixInStream =
                // shift the stream right
                ((0 until mixInPosition).asSequence().map { ZeroSample })
                        .plus(mixInStream.asSequence(sampleRate))
                        // extend the mixing stream to be not shorter than resulting stream
                        .plus((0 until (samplesCount() - mixInStream.samplesCount())).asSequence().map { ZeroSample })
        val length = (endIndex?.let { it + 1 } ?: samplesCount()) - startIndex
        return alignedSourceStream
                .drop(startIndex)
                .take(length)
                .zip(
                        alignedMixInStream
                                .drop(startIndex)
                                .take(length)
                )
                .map { sampleOf(it.first + it.second) }

    }

    override fun info(namespace: String?): Map<String, String> {
        val prefix = namespace?.let { "[$it] " } ?: ""
        return mixInStream.info("${prefix}Mix-in") + sourceStream.info("${prefix}Source") + mapOf(
                "${prefix}Samples count" to samplesCount().toString(),
                "${prefix}Mix In position" to mixInPosition.toString()
        )
    }

}