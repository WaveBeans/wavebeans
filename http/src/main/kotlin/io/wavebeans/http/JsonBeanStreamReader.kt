package io.wavebeans.http

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FunctionMergedStreamParams
import io.wavebeans.lib.stream.FunctionMergedStreamParamsSerializer
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule

/**
 * Reads a sequence of elements as a new-line separated JSON objects.
 *
 * The object must be serializable by `kotlinx.serialization`.
 * For example via annotation:
 *
 * ```kotlin
 * @Serializable
 * data class A(val value: Long)
 * ```
 *
 */
class JsonBeanStreamReader(
        stream: BeanStream<*>,
        sampleRate: Float,
        offset: TimeMeasure = 0.s
) : BeanStreamReader(stream, sampleRate, offset) {

    /**
     * The objects are read from the sequence as `Any`, need to detect serializer based
     * on actual value but not the compile-time type as it is done via regular API.
     */
    private object PlainObjectSerializer : KSerializer<Any> {
        override val descriptor: SerialDescriptor
            get() = object : SerialClassDescImpl("any") {}

        override fun deserialize(decoder: Decoder): Any {
            throw IllegalStateException("This serializer can only be used for serialization!")
        }

        override fun serialize(encoder: Encoder, obj: Any) {
            val s = serializerByTypeToken(obj::class.java)
            encoder.encode(s, obj)
        }
    }

    @Serializer(forClass = BeanStreamElement::class)
    private object BeanStreamElementSerializer : KSerializer<BeanStreamElement> {
        override val descriptor: SerialDescriptor
            get() = object : SerialClassDescImpl("any") {
                init {
                    addElement("offset")
                    addElement("value")
                }
            }

        override fun deserialize(decoder: Decoder): BeanStreamElement {
            throw IllegalStateException("This serializer can only be used for serialization!")
        }

        override fun serialize(encoder: Encoder, obj: BeanStreamElement ) {
            val s = encoder.beginStructure(descriptor)
            s.encodeLongElement(descriptor, 0, obj.offset.time)
            s.encodeSerializableElement(descriptor, 1, PlainObjectSerializer, obj.value)
            s.endStructure(descriptor)
        }

    }

    private val paramsModule = SerializersModule {
        this.contextual(BeanStreamElement::class, BeanStreamElementSerializer)
    }

    private val json = Json(JsonConfiguration.Stable, paramsModule)

    @ImplicitReflectionSerializer
    override fun stringifyObj(obj: BeanStreamElement): String = json.stringify(obj)
}