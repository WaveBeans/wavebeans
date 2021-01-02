package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.jvm.jvmName

/**
 * Flattens the stream of [SampleVector]. Flatten is a process that extracts a single stream of all elements to
 * the continuous stream of [SampleVector].
 *
 * @return the flattened stream of [Sample].
 */
@JvmName("flattenSampleVector")
fun BeanStream<SampleVector>.flatten(): BeanStream<Sample> = this.flatMap { it.asList() }

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
fun <T : Any, I : Any> BeanStream<I>.flatMap(map: (I) -> Iterable<T>): BeanStream<T> = this.flatMap(Fn.wrap(map))

/**
 * Flattens the stream of any type [T] from the any type [I] with help of extract function [map]. Flatten is a process
 * that extracts a single stream of all elements to the continuous stream of [T].
 *
 * @param map the function as [Fn] to extract the iterable type out of [I]. The argument is the one that was read out of
 *            stream on the iteration, the result is expected to be empty or non-empty iterable of [T].
 *
 * @param T the type of the resulted element.
 * @param I the type which will be used to extract out the [Iterable] of type [T].
 *
 * @return the flattened stream of [T].
 */
fun <T : Any, I : Any> BeanStream<I>.flatMap(map: Fn<I, Iterable<T>>): BeanStream<T> =
        FlattenStream(this, FlattenStreamsParams(map))

/**
 * Parameters to use with [FlattenStream].
 *
 * @param I the input type of the [map] function.
 * @param T the output type of operation.
 */
@Serializable(with = FlattenStreamsParamsSerializer::class)
class FlattenStreamsParams<I : Any, T : Any>(
        /**
         * the function as [Fn] to extract the iterable type out of [I]. The argument is the one that was read out of
         * stream on the iteration, the result is expected to be empty or non-empty iterable of [T].
         */
        val map: Fn<I, Iterable<T>>
) : BeanParams()

/**
 * The serializer for [FlattenStreamsParams].
 */
object FlattenStreamsParamsSerializer: KSerializer<FlattenStreamsParams<*, *>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FlattenStreamsParams::class.jvmName) {
        element("map", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): FlattenStreamsParams<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var map: Fn<*, *>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> map = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return FlattenStreamsParams(
                map!! as Fn<Any, Iterable<Any>>
        )
    }

    override fun serialize(encoder: Encoder, value: FlattenStreamsParams<*, *>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, value.map)
        structure.endStructure(descriptor)
    }
}

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
                        current = parameters.map.apply(iterator.next()).iterator()
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
                        current = parameters.map.apply(iterator.next()).iterator()
                    } else {
                        throw NoSuchElementException("No elements left")
                    }
                }
                return current!!.next()
            }
        }.asSequence()
    }
}