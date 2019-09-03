package mux.lib.stream

import mux.lib.Sample
import mux.lib.ZeroSample
import mux.lib.timeToSampleIndexCeil
import mux.lib.timeToSampleIndexFloor
import java.util.concurrent.TimeUnit

// TODO define different iterating strategies
fun FiniteStream.sampleStream(): SampleStream = FiniteSampleStream(this)

class FiniteSampleStream(
        val finiteStream: FiniteStream,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {
            val s = timeToSampleIndexFloor(start, timeUnit, sampleRate)
            val innerIterator = finiteStream
                    .asSequence(sampleRate)
                    .drop(s.toInt())
                    .iterator()

            var samplesLeft = end
                    ?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate) }
                    ?.minus(s)
                    ?: Long.MAX_VALUE

            override fun hasNext(): Boolean = true

            override fun next(): Sample =
                    if (innerIterator.hasNext() && samplesLeft-- > 0)
                        innerIterator.next()
                    else
                        ZeroSample


        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return FiniteSampleStream(finiteStream, start, end, timeUnit)
    }

}