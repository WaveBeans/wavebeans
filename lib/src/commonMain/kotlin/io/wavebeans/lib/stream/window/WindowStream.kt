package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.AbstractOperationBeanStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.properties.Delegates
import kotlin.properties.Delegates.notNull

/**
 * Creates a [BeanStream] of [Window] of type [Sample].
 *
 * @param size the size of the window. Must be more than 1.
 */
fun BeanStream<Sample>.window(size: Int): BeanStream<Window<Sample>> =
    this.window(size, size) { ZeroSample }

/**
 * Creates a [BeanStream] of [Window] of type [Sample].
 *
 * @param size the size of the window. Must be more than 1.
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 */
fun BeanStream<Sample>.window(size: Int, step: Int): BeanStream<Window<Sample>> =
    this.window(size, step) { ZeroSample }

/**
 * Creates a [BeanStream] of [Window] of specified type.
 *
 * @param size the size of the window. Must be more than 1.
 * @param zeroElFn function that creates zero element objects.
 */
fun <T : Any> BeanStream<T>.window(size: Int, zeroElFn: ExecutionScope.(Unit) -> T): BeanStream<Window<T>> =
    this.window(EmptyScope, size, size, zeroElFn)

/**
 * Creates a [BeanStream] of [Window] of specified type.
 *
 * @param size the size of the window. Must be more than 1.
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 * @param zeroElFn function that creates zero element objects.
 */
fun <T : Any> BeanStream<T>.window(size: Int, step: Int, zeroElFn: ExecutionScope.(Unit) -> T): BeanStream<Window<T>> =
    this.window(EmptyScope, size, step, zeroElFn)

/**
 * Creates a [BeanStream] of [Window] of specified type.
 *
 * @param scope the execution scope to use.
 * @param size the size of the window. Must be more than 1.
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 * @param zeroElFn function that creates zero element objects.
 */
fun <T : Any> BeanStream<T>.window(
    scope: ExecutionScope,
    size: Int,
    step: Int,
    zeroElFn: ExecutionScope.(Unit) -> T
): BeanStream<Window<T>> =
    WindowStream(this, WindowStreamParams(scope, size, step, zeroElFn))

/**
 * Parameters for [WindowStream].
 *
 * @param windowSize the size of the window. Must be more than 1.
 * @param step the size of the step to move window forward. For a fixed window should be the same as [windowSize]. Must be more or equal to 1.
 */
class WindowStreamParams<T : Any>(
    val scope: ExecutionScope,
    val windowSize: Int,
    val step: Int,
    val zeroElFn: ExecutionScope.(Unit) -> T
) : BeanParams {
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
 */
class WindowStream<T : Any>(
    override val input: BeanStream<T>,
    override val parameters: WindowStreamParams<T>
) : AbstractOperationBeanStream<T, Window<T>>(input), AlterBean<T, Window<T>>, SinglePartitionBean {

    override fun operationSequence(input: Sequence<T>, sampleRate: Float): Sequence<Window<T>> {
        return input
            .windowed(
                size = parameters.windowSize,
                step = parameters.step,
                partialWindows = true
            )
            .map {
                Window(
                    parameters.windowSize,
                    parameters.step,
                    it
                ) { parameters.zeroElFn.invoke(parameters.scope, Unit) }
            }
    }
}