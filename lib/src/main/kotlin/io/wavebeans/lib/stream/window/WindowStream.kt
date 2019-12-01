package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.SampleStream
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

/**
 * Creates a [WindowStream] from specified [SampleStream].
 *
 * @param size the size of the window. Must be more than 1.
 */
fun SampleStream.window(size: Int): SampleWindowStream =
        SampleWindowStreamImpl(this, WindowStreamParams(size, size))

/**
 * Converts a [WindowStream] to a sliding [WindowStream].
 *
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 */
fun SampleWindowStream.sliding(step: Int): SampleWindowStream =
        SampleWindowStreamImpl((this as SampleWindowStreamImpl).source, WindowStreamParams(this.parameters.windowSize, step))

/**
 * Parameters for [WindowStream].
 *
 * @param windowSize the size of the window. Must be more than 1.
 * @param step the size of the step to move window forward. For a fixed window should be the same as [windowSize]. Must be more or equal to 1.
 * @param start starting point in [timeUnit] for getting sub-range of the stream. Any value less than zero is allowed, but basically work as zero.
 * @param end ending point in [timeUnit] for getting sub-range of the stream. Null if the range is not limited.
 * @param timeUnit what time unit is used for [start] and [end] points. If not specified [TimeUnit.MILLISECONDS] is used.
 */
@Serializable
open class WindowStreamParams(
        val windowSize: Int,
        val step: Int,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams() {
    init {
        require(step >= 1) { "Step should be more or equal to 1" }
        require(windowSize > 1) { "Window size should be more than 1" }
    }
}


/**
 * The class provides windowed access to the underlying stream. The type of the stream can be Fixed and Sliding.
 * The difference is only how you define the [WindowStreamParams.step] -- if it's the same as [WindowStreamParams.windowSize]
 * it is considered to be Fixed, otherwise Sliding.
 *
 * Window stream supports a lot of operations, however make sure the window attributes are the same, otherwise you'll
 * get the runtime exception.
 *
 * This implementation works pretty much with any sampled medium, and uses generic [BeanStream] as source of data.
 *
 * @param T the type of the medium. Must be non-nullable type.
 * @param S the type of stream itself for some methods to return correct type.
 */
interface WindowStream<T : Any, WINDOWING_STREAM : Any, S : WindowStream<T, WINDOWING_STREAM, S>> : BeanStream<Window<T>, S> {

    override val parameters: WindowStreamParams

}

/**
 * Abstract implementation of generic window stream creator from any [BeanStream].
 */
abstract class AbstractWindowStream<T : Any, WINDOWING_STREAM : Any, S : WindowStream<T, WINDOWING_STREAM, S>>(
        val source: BeanStream<T, WINDOWING_STREAM>,
        val params: WindowStreamParams
) : WindowStream<T, WINDOWING_STREAM, S>, AlterBean<T, WINDOWING_STREAM, Window<T>, S>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<Window<T>> {
        val startIdx = timeToSampleIndexFloor(params.start, params.timeUnit, sampleRate).toInt() / params.step
        val endIdx = params.end?.let { timeToSampleIndexCeil(it, params.timeUnit, sampleRate).toInt() }
                ?.let { it / params.step + if (it % params.step == 0) 0 else 1 }
                ?: Int.MAX_VALUE

        return source.asSequence(sampleRate)
                .windowed(
                        size = params.windowSize,
                        step = params.step,
                        partialWindows = true
                )
                .drop(startIdx)
                .take(endIdx - startIdx)
                .map { Window(it) }

    }

    override fun inputs(): List<Bean<*, *>> = listOf(source)

    override val parameters: WindowStreamParams
        get() = params

}


/**
 * [WindowStream] interface that specifies types that it is working with [Sample]s which are coming from [SampleStream]
 * and its output stream is itself.
 */
interface SampleWindowStream : WindowStream<Sample, SampleStream, SampleWindowStream>

/**
 * [WindowStream] implementation to work with [Sample] as a medium
 */
class SampleWindowStreamImpl(
        source: BeanStream<Sample, SampleStream>,
        params: WindowStreamParams
) : AbstractWindowStream<Sample, SampleStream, SampleWindowStream>(source, params), SampleWindowStream {

    override val input: Bean<Sample, SampleStream>
        get() = source

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleWindowStreamImpl {
        return SampleWindowStreamImpl(source, WindowStreamParams(params.windowSize, params.step, start, end, timeUnit))
    }

}