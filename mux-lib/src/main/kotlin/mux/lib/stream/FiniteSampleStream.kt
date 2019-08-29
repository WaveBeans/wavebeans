package mux.lib.stream

import mux.lib.Sample
import mux.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

// TODO define more read strategies, i.e. repeat N times and so on
class FiniteSampleStream(
        val input: FiniteInput
) : SampleStream {

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteSampleStream =
            FiniteSampleStream(input.rangeProjection(start, end, timeUnit))

    fun samplesCount(): Int = input.samplesCount()

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long = input.length(timeUnit)

}