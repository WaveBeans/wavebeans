package io.wavebeans.lib.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

fun <T : Any, R : Any> BeanStream<T>.map(transform: (T) -> R): BeanStream<R> = this.map(wrap(transform))
fun <T : Any, R : Any> BeanStream<T>.map(transform: Fn<T, R>): BeanStream<R> = MapStream(this, MapStreamParams(transform))

object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MapStreamParams::class.className()) {
        element("transformFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var fn: Fn<Any, Any>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> fn = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any, Any>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            MapStreamParams(fn)
        }
    }

    override fun serialize(encoder: Encoder, value: MapStreamParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, FnSerializer, value.transform)
        }
    }

}

@Serializable(with = MapStreamParamsSerializer::class)
data class MapStreamParams<T : Any, R : Any>(val transform: Fn<T, R>) : BeanParams

class MapStream<T : Any, R : Any>(
        override val input: BeanStream<T>,
        override val parameters: MapStreamParams<T, R>
) : AbstractOperationBeanStream<T, R>(input), AlterBean<T, R> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun operationSequence(input: Sequence<T>, sampleRate: Float): Sequence<R> {
        log.trace { "[$this] Initiating sequence Map(input = $input,parameters = $parameters)" }
        return input.map { parameters.transform.apply(it) }
    }

}