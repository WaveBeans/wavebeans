package mux.lib.stream

import mux.lib.Sample
import mux.lib.timeToSampleIndexCeil
import mux.lib.timeToSampleIndexFloor
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

class MergedSampleStream(
        val sourceStream: SampleStream,
        val mergingStream: SampleStream,
        val shift: Int,
        val operation: (Sample, Sample) -> Sample,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream {

    override fun info(namespace: String?): Map<String, String> {
        val prefix = namespace?.let { "[$it] " } ?: ""
        return mergingStream.info("${prefix}Merging") + sourceStream.info("${prefix}Source") + mapOf(
                "${prefix}Samples count" to samplesCount().toString(),
                "${prefix}Shift" to shift.toString()
        )
    }

    override fun samplesCount(): Int {
        return countSamples(true)
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return MergedSampleStream(sourceStream, mergingStream, shift, operation, start, end, timeUnit)
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIndex = max(timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt(), 0)
        val endIndex = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() }

        // note: streams should be aligned by length as zip() uses the shortest one
        val samplesCount = countSamples(false)
        val alignedSourceStream = sourceStream.asSequence(sampleRate)
                .zeroPadLeft(if (shift >= 0) 0 else abs(shift))
                .zeroPadRight(samplesCount - sourceStream.samplesCount())

        val alignedMixInStream = mergingStream.asSequence(sampleRate)
                .zeroPadLeft(if (shift >= 0) shift else 0)
                .zeroPadRight(samplesCount - mergingStream.samplesCount())

        val length = endIndex?.let { it - startIndex } ?: Int.MAX_VALUE
        return alignedSourceStream
                .drop(startIndex)
                .take(length)
                .zip(
                        alignedMixInStream
                                .drop(startIndex)
                                .take(length)
                )
                .map { operation(it.first, it.second) }
    }


    private fun countSamples(onProjection: Boolean): Int {
        val mixInSampleCount = if (onProjection && (start > 0 || end != null)) {
            mergingStream.rangeProjection(start, end, timeUnit).samplesCount()
        } else {
            mergingStream.samplesCount()
        }
        val sourceSampleCount = if (onProjection && (start > 0 || end != null)) {
            sourceStream.rangeProjection(start, end, timeUnit).samplesCount()
        } else {
            sourceStream.samplesCount()
        }

        return if (shift >= 0) {
            max(mixInSampleCount + abs(shift), sourceSampleCount)
        } else {
            max(mixInSampleCount + abs(shift), sourceSampleCount + abs(shift))
        }
    }


}

fun diff(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(x, y, shift, { a, b -> a - b })

fun sum(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(x, y, shift, { a, b -> a + b })