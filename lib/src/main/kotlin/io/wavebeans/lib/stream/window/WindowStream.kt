package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable

/**
 * Creates a [WindowStream] from specified [SampleStream].
 *
 * @param size the size of the window. Must be more than 1.
 */
fun BeanStream<Sample>.window(size: Int): WindowStream<Sample> =
        SampleWindowStream(this, WindowStreamParams(size, size))

/**
 * Converts a [WindowStream] to a sliding [WindowStream].
 *
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 */
fun WindowStream<Sample>.sliding(step: Int): WindowStream<Sample> =
        SampleWindowStream((this as SampleWindowStream).source, WindowStreamParams(this.parameters.windowSize, step))

/**
 * Parameters for [WindowStream].
 *
 * @param windowSize the size of the window. Must be more than 1.
 * @param step the size of the step to move window forward. For a fixed window should be the same as [windowSize]. Must be more or equal to 1.
 */
@Serializable
open class WindowStreamParams(
        val windowSize: Int,
        val step: Int
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
interface WindowStream<T : Any> : BeanStream<Window<T>> {

    override val parameters: WindowStreamParams

}

/**
 * Abstract implementation of generic window stream creator from any [BeanStream].
 */
abstract class AbstractWindowStream<T : Any>(
        val source: BeanStream<T>,
        val params: WindowStreamParams
) : WindowStream<T>, AlterBean<T, Window<T>>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<Window<T>> {
        return source.asSequence(sampleRate)
                .windowed(
                        size = params.windowSize,
                        step = params.step,
                        partialWindows = true
                )
                .map { Window(it) }

    }

    override fun inputs(): List<AnyBean> = listOf(source)

    override val parameters: WindowStreamParams
        get() = params

    override val input: Bean<T>
        get() = source
}

/**
 * [WindowStream] implementation to work with [Sample] as a medium
 */
class SampleWindowStream(
        source: BeanStream<Sample>,
        params: WindowStreamParams
) : AbstractWindowStream<Sample>(source, params)