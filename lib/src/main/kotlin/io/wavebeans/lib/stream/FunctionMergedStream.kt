package io.wavebeans.lib.stream

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.MultiAlterBean
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.jvm.jvmName

fun <T : Any, R : Any> BeanStream<T>.merge(with: BeanStream<T>, merge: (T?, T?) -> R): BeanStream<R> =
        FunctionMergedStream(this, with, FunctionMergedStreamParams(merge))


object FunctionMergedStreamParamsSerializer : KSerializer<FunctionMergedStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("FunctionMergedStreamParams") {
        init {
            addElement("mergeFn")
        }
    }

    override fun deserialize(decoder: Decoder): FunctionMergedStreamParams<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var funcMergeClazzName: String? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> funcMergeClazzName = dec.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST") val funcMergeByName = Class.forName(funcMergeClazzName).newInstance() as (Any?, Any?) -> Any
        return FunctionMergedStreamParams(funcMergeByName)
    }

    override fun serialize(encoder: Encoder, obj: FunctionMergedStreamParams<*, *>) {
        val funcMergeName = obj.merge::class.jvmName
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, funcMergeName)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = FunctionMergedStreamParamsSerializer::class)
class FunctionMergedStreamParams<T : Any, R : Any>(
        val merge: (T?, T?) -> R
) : BeanParams()

class FunctionMergedStream<T : Any, R : Any>(
        val sourceStream: BeanStream<T>,
        val mergeStream: BeanStream<T>,
        override val parameters: FunctionMergedStreamParams<T, R>
) : BeanStream<R>, MultiAlterBean<T, R> {

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

                return parameters.merge(s, m)
            }

        }.asSequence()
    }
}

