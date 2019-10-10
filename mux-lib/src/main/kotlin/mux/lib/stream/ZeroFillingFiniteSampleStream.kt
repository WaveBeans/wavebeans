package mux.lib.stream

import kotlinx.serialization.Serializable
import mux.lib.*
import java.util.concurrent.TimeUnit

class ZeroFilling : FiniteToStream {
    override fun convert(finiteSampleStream: FiniteSampleStream): SampleStream {
        return ZeroFillingFiniteSampleStream(finiteSampleStream, ZeroFillingFiniteSampleStreamParams())
    }
}

@Serializable
data class ZeroFillingFiniteSampleStreamParams(
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

private class ZeroFillingFiniteSampleStream(
        val finiteSampleStream: FiniteSampleStream,
        val params: ZeroFillingFiniteSampleStreamParams
) : SampleStream, AlterBean<SampleArray, FiniteSampleStream, SampleArray, SampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<SampleArray, FiniteSampleStream> = finiteSampleStream

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
//        return object : Iterator<Sample> {
//            val s = timeToSampleIndexFloor(params.start, params.timeUnit, sampleRate)
//            val innerIterator = finiteSampleStream
//                    .asSequence(sampleRate)
//                    .drop(s.toInt())
//                    .iterator()
//
//            var samplesLeft = params.end
//                    ?.let { timeToSampleIndexCeil(it, params.timeUnit, sampleRate) }
//                    ?.minus(s)
//                    ?: Long.MAX_VALUE
//
//            override fun hasNext(): Boolean = true
//
//            override fun next(): Sample =
//                    if (innerIterator.hasNext() && samplesLeft-- > 0)
//                        innerIterator.next()
//                    else
//                        ZeroSample
//
//
//        }.asSequence()
        TODO()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ZeroFillingFiniteSampleStream(finiteSampleStream, ZeroFillingFiniteSampleStreamParams(start, end, timeUnit))
    }

}