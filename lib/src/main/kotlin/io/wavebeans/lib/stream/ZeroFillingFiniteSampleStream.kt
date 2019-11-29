package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
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
) : SampleStream, AlterBean<Sample, FiniteSampleStream, Sample, SampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample, FiniteSampleStream> = finiteSampleStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            var toSkip = timeToSampleIndexFloor(params.start, params.timeUnit, sampleRate)
            val iterator = finiteSampleStream
                    .asSequence(sampleRate)
                    .iterator()

            var samplesLeft = params.end
                    ?.let { timeToSampleIndexCeil(it, params.timeUnit, sampleRate) }
                    ?: Long.MAX_VALUE

            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                var el: Sample
                do {
                    el = if (iterator.hasNext() && samplesLeft-- > 0) { // there is something left to read
                        iterator.next()
                    } else {
                        ZeroSample
                    }
                    toSkip--
                } while (toSkip >= 0)

                return el
            }
        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ZeroFillingFiniteSampleStream(finiteSampleStream, ZeroFillingFiniteSampleStreamParams(start, end, timeUnit))
    }

}