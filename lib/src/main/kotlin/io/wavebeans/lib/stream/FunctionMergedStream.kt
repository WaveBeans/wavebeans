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

fun <T1 : Any, T2 : Any, R : Any> BeanStream<T1>.merge(with: BeanStream<T2>, merge: (Pair<T1?, T2?>) -> R?): BeanStream<R> =
        this.merge(with, Fn.wrap(merge))

fun <T1 : Any, T2 : Any, R : Any> BeanStream<T1>.merge(with: BeanStream<T2>, merge: Fn<Pair<T1?, T2?>, R?>): BeanStream<R> =
        FunctionMergedStream(this, with, FunctionMergedStreamParams(merge))


object FunctionMergedStreamParamsSerializer : KSerializer<FunctionMergedStreamParams<*, *, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FunctionMergedStreamParams::class.jvmName) {
        element("mergeFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): FunctionMergedStreamParams<*, *, *> {
        val dec = decoder.beginStructure(descriptor)
        var fn: Fn<*, *>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> fn = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return FunctionMergedStreamParams(fn as Fn<Pair<Any?, Any?>, Any?>)
    }

    override fun serialize(encoder: Encoder, value: FunctionMergedStreamParams<*, *, *>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, value.merge)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = FunctionMergedStreamParamsSerializer::class)
class FunctionMergedStreamParams<T1 : Any, T2 : Any, R : Any>(
        val merge: Fn<Pair<T1?, T2?>, R?>
) : BeanParams

@Suppress("UNCHECKED_CAST")
class FunctionMergedStream<T1 : Any, T2 : Any, R : Any>(
        sourceStream: BeanStream<T1>,
        mergeStream: BeanStream<T2>,
        override val parameters: FunctionMergedStreamParams<T1, T2, R>
) : AbstractMultiOperationBeanStream<R>(listOf(sourceStream, mergeStream) as List<BeanStream<Any>>), MultiAlterBean<R>, SinglePartitionBean {

    override val inputs: List<AnyBean> by lazy { listOf(sourceStream, mergeStream) }

    override fun operationSequence(inputs: List<Sequence<Any>>, sampleRate: Float): Sequence<R> {
        val sourceIterator = inputs.first().iterator()
        val mergeIterator = inputs.drop(1).first().iterator()
        return object : Iterator<R> {

            var nextEl: R? = null

            override fun hasNext(): Boolean {
                return if (isNextElAvailable()) {
                    advance()
                    nextEl != null
                } else {
                    false
                }
            }

            override fun next(): R {
                if (!isNextElAvailable()) throw NoSuchElementException("No more elements to read")
                advance()
                val el = nextEl ?: throw NoSuchElementException("No more elements to read")
                nextEl = null
                return el
            }

            private fun isNextElAvailable() = nextEl != null || sourceIterator.hasNext() || mergeIterator.hasNext()

            private fun advance() {
                if (nextEl == null) {
                    val s = if (sourceIterator.hasNext()) sourceIterator.next() else null
                    val m = if (mergeIterator.hasNext()) mergeIterator.next() else null
                    nextEl = parameters.merge.apply(Pair(s as T1?, m as T2?))
                }
            }
        }.asSequence()
    }
}

