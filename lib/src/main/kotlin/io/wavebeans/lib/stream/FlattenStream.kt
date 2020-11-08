package io.wavebeans.lib.stream

import io.wavebeans.lib.*

@JvmName("flattenSampleVector")
fun BeanStream<SampleVector>.flatten(): BeanStream<Sample> = this.flatMap { it.asList() }

fun <T : Any, I : Iterable<T>> BeanStream<I>.flatten(): BeanStream<T> = this.flatMap { it }

fun <T : Any, I : Any> BeanStream<I>.flatMap(map: (I) -> Iterable<T>): BeanStream<T> = this.flatMap(Fn.wrap(map))

fun <T : Any, I : Any> BeanStream<I>.flatMap(map: Fn<I, Iterable<T>>): BeanStream<T> =
        FlattenStream(this, FlattenStreamsParams(map))

class FlattenStreamsParams<I : Any, T : Any>(
        val map: Fn<I, Iterable<T>>
) : BeanParams()

class FlattenStream<I : Any, T : Any>(
        override val input: BeanStream<I>,
        override val parameters: FlattenStreamsParams<I, T>
) : BeanStream<T>, AlterBean<I, T>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val iterator = input.asSequence(sampleRate).iterator()
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