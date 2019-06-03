package mux.lib.stream

import mux.lib.timeToSampleIndexCeil
import mux.lib.timeToSampleIndexFloor
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class MixedSampleStream(
        val sourceStream: SampleStream,
        val mixInStream: SampleStream,
        val mixInPosition: Int,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream {

    init {
        if (mixInPosition < 0) throw SampleStreamException("Position can't be negative")
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return MixedSampleStream(sourceStream, mixInStream, mixInPosition, start, end, timeUnit)
    }

    override fun samplesCount(): Int {
        val mixInSampleCount = if (start > 0 || end != null) {
            mixInStream.rangeProjection(start, end, timeUnit).samplesCount()
        } else {
            mixInStream.samplesCount()
        }
        val sourceSampleCount = if (start > 0 || end != null) {
            sourceStream.rangeProjection(start, end, timeUnit).samplesCount()
        } else {
            sourceStream.samplesCount()
        }
        return max(mixInSampleCount + mixInPosition, sourceSampleCount)
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIndex = max(timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt(), 0)
        val endIndex = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() }
        // note: streams should be aligned by length as zip() uses the shortest one
        val alignedSourceStream = sourceStream.asSequence(sampleRate)
                // extend the stream to fit all samples if it's shorter
                .zeroPadRight(samplesCount() - sourceStream.samplesCount())
        val alignedMixInStream =
                mixInStream.asSequence(sampleRate)
                        .zeroPadLeft(mixInPosition)
                        // extend the mixing stream to be not shorter than resulting stream
                        .zeroPadRight(samplesCount() - mixInStream.samplesCount())
        val length = endIndex?.let { it - startIndex } ?: Int.MAX_VALUE
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