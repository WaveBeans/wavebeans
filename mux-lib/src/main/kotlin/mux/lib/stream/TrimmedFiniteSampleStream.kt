package mux.lib.stream

import kotlinx.serialization.Serializable
import mux.lib.*
import mux.lib.Bean
import java.util.concurrent.TimeUnit

fun SampleStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteSampleStream =
        TrimmedFiniteSampleStream(this, TrimmedFiniteSampleStreamParams(length, timeUnit))

@Serializable
data class TrimmedFiniteSampleStreamParams(
        val length: Long,
        val timeUnit: TimeUnit
) : BeanParams()

class TrimmedFiniteSampleStream(
        val sampleStream: SampleStream,
        val params: TrimmedFiniteSampleStreamParams
) : FiniteSampleStream, AlterBean<SampleArray, SampleStream, SampleArray, FiniteSampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<SampleArray, SampleStream> = sampleStream

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
        var samplesToTake = timeToSampleIndexFloor(TimeUnit.NANOSECONDS.convert(params.length, params.timeUnit), TimeUnit.NANOSECONDS, sampleRate)
        val iterator = sampleStream.asSequence(sampleRate).iterator()

        return object : Iterator<SampleArray> {

            override fun hasNext(): Boolean =
                    samplesToTake > 0 && iterator.hasNext()

            override fun next(): SampleArray {
                val a = iterator.next()
                return if (samplesToTake < a.size) {
                    samplesToTake = 0
                    a.copyOf(samplesToTake.toInt())
                } else {
                    samplesToTake -= a.size
                    a
                }
            }

        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): TrimmedFiniteSampleStream =
            TrimmedFiniteSampleStream(
                    sampleStream.rangeProjection(start, end, timeUnit),
                    params.copy(
                            length = end?.minus(start) ?: (timeUnit.convert(params.length, params.timeUnit) - start)
                    )
            )

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(params.length, params.timeUnit)

}
