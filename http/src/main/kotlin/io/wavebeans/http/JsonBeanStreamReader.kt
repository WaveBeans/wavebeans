package io.wavebeans.http

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.jvm.jvmName

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
        override val descriptor: SerialDescriptor = SerialDescriptor("Any") {}

        override fun deserialize(decoder: Decoder): Any {
            throw IllegalStateException("This serializer can only be used for serialization!")
        }

        override fun serialize(encoder: Encoder, value: Any) {
            val s = serializerByTypeToken(value::class.java)
            encoder.encode(s, value)
        }
    }

    @Serializer(forClass = BeanStreamElement::class)
    private object BeanStreamElementSerializer : KSerializer<BeanStreamElement> {
        override val descriptor: SerialDescriptor = SerialDescriptor(BeanStreamElement::class.jvmName) {
                    element("offset", Long.serializer().descriptor)
                    element("value", PlainObjectSerializer.descriptor)
            }

        override fun deserialize(decoder: Decoder): BeanStreamElement {
            throw IllegalStateException("This serializer can only be used for serialization!")
        }

        override fun serialize(encoder: Encoder, value: BeanStreamElement ) {
            val s = encoder.beginStructure(descriptor)
            s.encodeLongElement(descriptor, 0, value.offset.time)
            s.encodeSerializableElement(descriptor, 1, PlainObjectSerializer, value.value)
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