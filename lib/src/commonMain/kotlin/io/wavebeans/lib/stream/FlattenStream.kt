package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

/**
 * Flattens the stream of [SampleVector]. Flatten is a process that extracts a single stream of all elements to
 * the continuous stream of [SampleVector].
 *
 * @return the flattened stream of [Sample].
 */
@JvmName("flattenSampleVector")
@JsName("flattenSampleVector")
fun BeanStream<SampleVector>.flatten(): BeanStream<Sample> = this.flatMap { it.asIterable() }

/**
 * Flattens the stream of any type [T] from the iterable type [I]. Flatten is a process that extracts a single stream
 * of all elements to the continuous stream of [T].
 *
 * @param T the type of the resulted element.
 * @param I the type which extends the [Iterable] of type [T] to yield elements from.
 *
 * @return the flattened stream of [T].
 */
fun <T : Any, I : Iterable<T>> BeanStream<I>.flatten(): BeanStream<T> = this.flatMap { it }

/**
 * Flattens the stream of any type [T] from the any type [I] with help of extract function [map]. Flatten is a process
 * that extracts a single stream of all elements to the continuous stream of [T].
 *
 * @param map the function to extract the iterable type out of [I]. The argument is the one that was read out of
 *            stream on the iteration, the result is expected to be empty or non-empty iterable of [T].
 *
 * @param T the type of the resulted element.
 * @param I the type which will be used to extract out the [Iterable] of type [T].
 *
 * @return the flattened stream of [T].
 */
fun <T : Any, I : Any> BeanStream<I>.flatMap(map: ExecutionScope.(I) -> Iterable<T>): BeanStream<T> =
    this.flatMap(EmptyScope, map)

/**
 * Flattens the stream of any type [T] from the any type [I] with help of extract function [map]. Flatten is a process
 * that extracts a single stream of all elements to the continuous stream of [T].
 *
 * @param scope the execution scope to use.
 * @param map the function to extract the iterable type out of [I]. The argument is the one that was read out of
 *            stream on the iteration, the result is expected to be empty or non-empty iterable of [T].
 *
 * @param T the type of the resulted element.
 * @param I the type which will be used to extract out the [Iterable] of type [T].
 *
 * @return the flattened stream of [T].
 */
fun <T : Any, I : Any> BeanStream<I>.flatMap(
    scope: ExecutionScope,
    map: ExecutionScope.(I) -> Iterable<T>
): BeanStream<T> = FlattenStream(this, FlattenStreamsParams(scope, map))


/**
 * Parameters to use with [FlattenStream].
 *
 * @param I the input type of the [map] function.
 * @param T the output type of operation.
 */
class FlattenStreamsParams<I : Any, T : Any>(
    /**
     * The execution scope.
     */
    val scope: ExecutionScope,
    /**
     * the function to extract the iterable type out of [I]. The argument is the one that was read out of
     * stream on the iteration, the result is expected to be empty or non-empty iterable of [T].
     */
    val map: ExecutionScope.(I) -> Iterable<T>
) : BeanParams

/**
 * Flattens the stream of any type [T] from the any type [I] with help of extract function [map]. Flatten is a process
 * that extracts a single stream of all elements to the continuous stream of [T].
 *
 * @param T the type of the resulted element.
 * @param I the type which will be used to extract out the [Iterable] of type [T].
 * @param input the stream to read from.
 * @param parameters the instance of [FlattenStreamsParams] to get the tuning parameters from.
 */
class FlattenStream<I : Any, T : Any>(
    override val input: BeanStream<I>,
    override val parameters: FlattenStreamsParams<I, T>
) : AbstractOperationBeanStream<I, T>(input), BeanStream<T>, AlterBean<I, T>, SinglePartitionBean {

    override fun operationSequence(input: Sequence<I>, sampleRate: Float): Sequence<T> {
        val iterator = input.iterator()
        return object : Iterator<T> {

            var current: Iterator<T>? = null

            override fun hasNext(): Boolean {
                while (true) {
                    if (current != null && current!!.hasNext()) {
                        return true
                    }
                    if ((current == null || !current!!.hasNext()) && iterator.hasNext()) {
                        current = parameters.map.invoke(parameters.scope, iterator.next()).iterator()
                    } else {
                        return false
                    }
                }
            }

            override fun next(): T {
                while (true) {
                    if (current != null && current!!.hasNext()) {
                        break
                    }
                    if (current == null && iterator.hasNext()) {
                        current = parameters.map.invoke(parameters.scope, iterator.next()).iterator()
                    } else {
                        throw NoSuchElementException("No elements left")
                    }
                }
                return current!!.next()
            }
        }.asSequence()
    }
}