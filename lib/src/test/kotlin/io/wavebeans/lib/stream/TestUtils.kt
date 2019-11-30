package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.Window
import java.util.concurrent.TimeUnit

fun IntRange.stream() = IntStream(this.toList())

fun Sequence<Sample>.asInts() = this.map { it.asInt() }
fun Sequence<Window<Sample>>.asGroupedInts() = this.map { it.elements.map { it.asInt() } }

class IntStream(
        val seq: List<Int>,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream {
    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIdx = timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt()
        val endIdx = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() } ?: Int.MAX_VALUE
        return seq
                .drop(startIdx)
                .take(endIdx - startIdx)
                .asSequence().map { sampleOf(it) }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): IntStream = IntStream(seq, start, end, timeUnit)

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

}
