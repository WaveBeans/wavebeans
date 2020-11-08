package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.io.WavFileOutputParams
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.jvm.jvmName

@JvmName("flattenWindow")
inline fun <reified T : Any> BeanStream<Window<T>>.flatten(): BeanStream<T> =
        FlattenWindowStream(
                this,
                FlattenWindowStreamsParams(
                        when (T::class) {
                            Sample::class -> Fn.wrap { (a, b) -> (a as Sample + b as Sample) as T }
                            SampleVector::class -> Fn.wrap { (a, b) -> (a as SampleVector + b as SampleVector) as T }
                            else -> Fn.wrap { throw IllegalStateException("Overlap resolve function should be specified") }
                        }
                )
        )

@JvmName("flattenWindow")
fun <T : Any> BeanStream<Window<T>>.flatten(overlapResolve: (Pair<T, T>) -> T): BeanStream<T> =
        this.flatten(Fn.wrap(overlapResolve))

@JvmName("flattenWindow")
fun <T : Any> BeanStream<Window<T>>.flatten(overlapResolve: Fn<Pair<T, T>, T>): BeanStream<T> =
        FlattenWindowStream(this, FlattenWindowStreamsParams(overlapResolve))

@Serializable(with = FlattenWindowStreamsParamsSerializer::class)
class FlattenWindowStreamsParams<T : Any>(
        val overlapResolve: Fn<Pair<T, T>, T>
) : BeanParams()

object FlattenWindowStreamsParamsSerializer: KSerializer<FlattenWindowStreamsParams<*>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FlattenWindowStreamsParams::class.jvmName) {
        element("overlapResolve", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): FlattenWindowStreamsParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var overlapResolve: Fn<*, *>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> overlapResolve = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return FlattenWindowStreamsParams(
                overlapResolve!! as Fn<Pair<Any, Any>, Any>
        )
    }

    override fun serialize(encoder: Encoder, value: FlattenWindowStreamsParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, value.overlapResolve)
        structure.endStructure(descriptor)
    }

}


class FlattenWindowStream<T : Any>(
        override val input: BeanStream<Window<T>>,
        override val parameters: FlattenWindowStreamsParams<T>
) : BeanStream<T>, AlterBean<Window<T>, T>, SinglePartitionBean {

    private class WindowEl<T : Any>(
            val window: Window<T>,
            val size: Int = window.size,
            val step: Int = window.step,
            var index: Int = 0,
    ) {
        operator fun get(index: Int): T {
            return if (index >= 0 && index < window.elements.size)
                window.elements[index]
            else
                window.zeroEl()
        }
    }

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val iterator: Iterator<Window<T>> = input.asSequence(sampleRate).iterator()
        return object : Iterator<T> {

            private var current: WindowEl<T>? = null
            private var following: WindowEl<T>? = null

            override fun hasNext(): Boolean {
                while (true) {
                    if (current != null && currentHasNext()) {
                        return true
                    }
                    if ((current == null || !currentHasNext()) && iterator.hasNext()) {
                        readCurrent()
                    } else if (following != null && followingHasNext()) {
                        //read the tail
                        return true
                    } else {
                        return false
                    }
                }
            }

            override fun next(): T {
                while (true) {
                    if (current != null && currentHasNext()) {
                        break
                    }
                    if (current == null && iterator.hasNext()) {
                        readCurrent()
                    } else if (following != null && followingHasNext()) {
                        //read the tail
                        readCurrent()
                    } else {
                        throw NoSuchElementException("No elements left")
                    }
                }
                return currentNext()
            }

            private fun readCurrent() {
                require(iterator.hasNext() || following != null) {
                    "Require iterator to have following element or following element being read"
                }
                if (following == null) {
                    current = WindowEl(iterator.next())
                } else {
                    current = following
                    following = null
                }
            }

            private fun readFollowing() {
                require(iterator.hasNext()) { "Require iterator to have following element" }
                following = WindowEl(iterator.next())
            }

            private fun currentHasNext(): Boolean {
                val c = checkNotNull(current) { "Require current to be not null" }
                return c.index < c.size || (c.step >= c.size && c.index < c.step)
            }

            private fun followingHasNext(): Boolean {
                val f = checkNotNull(following) { "Require following to be not null" }
                return f.index < f.size
            }

            private fun followingNext(): T {
                val f = checkNotNull(following) { "Require following to be not null" }
                return f[f.index++]
            }

            private fun currentNext(): T {
                val c = checkNotNull(current) { "Require current to be not null" }

                val overlapEl = if (c.index >= c.step) {
                    if ((following == null || !followingHasNext()) && iterator.hasNext()) {
                        do {
                            readFollowing()
                        } while (!followingHasNext() && iterator.hasNext())
                    }
                    if (following != null && followingHasNext()) {
                        followingNext()
                    } else {
                        null
                    }
                } else {
                    null
                }
                val el = c[c.index++]
                return overlapEl?.let { parameters.overlapResolve.apply(it to el) } ?: el

            }

        }.asSequence()
    }

}