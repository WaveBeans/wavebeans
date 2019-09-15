package mux.lib.stream

import mux.lib.*
import mux.lib.Mux
import mux.lib.MuxNode
import mux.lib.SingleMuxNode
import java.util.concurrent.TimeUnit

class ZeroFilling : FiniteToStream {
    override fun convert(finiteSampleStream: FiniteSampleStream): SampleStream {
        return ZeroFillingFiniteSampleStream(finiteSampleStream)
    }
}

private class ZeroFillingFiniteSampleStream(
        val finiteSampleStream: FiniteSampleStream,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream, AlterMuxNode<Sample, FiniteSampleStream, Sample, SampleStream> {

    override val input: MuxNode<Sample, FiniteSampleStream> = finiteSampleStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {
            val s = timeToSampleIndexFloor(start, timeUnit, sampleRate)
            val innerIterator = finiteSampleStream
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
        return ZeroFillingFiniteSampleStream(finiteSampleStream, start, end, timeUnit)
    }

}