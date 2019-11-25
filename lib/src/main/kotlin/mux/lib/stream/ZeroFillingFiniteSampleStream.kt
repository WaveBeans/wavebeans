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
        return object : Iterator<SampleArray> {

            var toSkip = timeToSampleIndexFloor(params.start, params.timeUnit, sampleRate)
            val innerArrayIterator = finiteSampleStream
                    .asSequence(sampleRate)
                    .iterator()

            var innerArrayIdx = 0
            var innerArray: SampleArray? = null

            var samplesLeft = params.end
                    ?.let { timeToSampleIndexCeil(it, params.timeUnit, sampleRate) }
                    ?: Long.MAX_VALUE

            override fun hasNext(): Boolean = true

            override fun next(): SampleArray = createSampleArray {
                var el: Sample
                do {
                    if (samplesLeft-- > 0) { // there is something left to read
                        if (innerArrayIdx >= innerArray?.size ?: 0) { // we need to read next element
                            if (innerArrayIterator.hasNext()) {
                                innerArray = innerArrayIterator.next()
                                innerArrayIdx = 0
                            } else {
                                innerArray = null
                            }
                        }
                        // inner array is in a good shape
                        el = innerArray?.get(innerArrayIdx++) ?: ZeroSample
                    } else {
                        el = ZeroSample
                    }
                    toSkip--
                } while (toSkip >= 0)

                el
            }
        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ZeroFillingFiniteSampleStream(finiteSampleStream, ZeroFillingFiniteSampleStreamParams(start, end, timeUnit))
    }

}