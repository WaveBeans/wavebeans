package mux.lib.stream

import mux.lib.Sample
import mux.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

class FiniteInputStream(
        val input: FiniteInput
) : FiniteStream {

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInputStream =
            FiniteInputStream(input.rangeProjection(start, end, timeUnit))

    override fun length(timeUnit: TimeUnit): Long = input.length(timeUnit)

}

