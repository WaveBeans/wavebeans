package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl

fun <T : Any, R : Any> BeanStream<T>.merge(with: BeanStream<T>, merge: (Pair<T?, T?>) -> R): BeanStream<R> =
        this.merge(with, Fn.wrap(merge))

fun <T : Any, R : Any> BeanStream<T>.merge(with: BeanStream<T>, merge: Fn<Pair<T?, T?>, R>): BeanStream<R> =
        FunctionMergedStream(this, with, FunctionMergedStreamParams(merge))


object FunctionMergedStreamParamsSerializer : KSerializer<FunctionMergedStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("FunctionMergedStreamParams") {
        init {
            addElement("mergeFn")
        }
    }

    override fun deserialize(decoder: Decoder): FunctionMergedStreamParams<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var fn: Fn<Pair<Any?, Any?>, Any>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> fn = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Any?, Any?>, Any>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return FunctionMergedStreamParams(fn!!)
    }

    override fun serialize(encoder: Encoder, obj: FunctionMergedStreamParams<*, *>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, obj.merge)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = FunctionMergedStreamParamsSerializer::class)
class FunctionMergedStreamParams<T : Any, R : Any>(
        val merge: Fn<Pair<T?, T?>, R>
) : BeanParams()

class FunctionMergedStream<T : Any, R : Any>(
        val sourceStream: BeanStream<T>,
        val mergeStream: BeanStream<T>,
        override val parameters: FunctionMergedStreamParams<T, R>
) : BeanStream<R>, MultiAlterBean<T, R>, SinglePartitionBean {

    override val inputs: List<Bean<T>>
        get() = listOf(sourceStream, mergeStream)

    override fun asSequence(sampleRate: Float): Sequence<R> {
        val sourceIterator = sourceStream.asSequence(sampleRate).iterator()
        val mergeIterator = mergeStream.asSequence(sampleRate).iterator()
        return object : Iterator<R> {

            override fun hasNext(): Boolean {
                return sourceIterator.hasNext() || mergeIterator.hasNext()
            }

            override fun next(): R {
                check(sourceIterator.hasNext() || mergeIterator.hasNext()) { "No more elements in the stream" }

                val s = if (sourceIterator.hasNext()) sourceIterator.next() else null
                val m = if (mergeIterator.hasNext()) mergeIterator.next() else null

                return parameters.merge.apply(Pair(s, m))
            }

        }.asSequence()
    }
}

