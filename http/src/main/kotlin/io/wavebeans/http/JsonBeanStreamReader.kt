package io.wavebeans.http

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
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

    companion object {
        private val registeredSerializers = mutableMapOf<KClass<*>, KSerializer<Any>>()

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> register(clazz: KClass<T>, serializer: KSerializer<T>) {
            registeredSerializers.putIfAbsent(clazz, serializer as KSerializer<Any>)
                    ?.let { throw IllegalStateException("$clazz has already registered serializer $it") }
        }

        fun find(clazz: KClass<*>): KSerializer<Any>? = registeredSerializers
                .filter { it.key == clazz || it.key.isSuperclassOf(clazz) }
                .map { it.value }
                .firstOrNull()

        init {
            register(FftSample::class, FftSampleSerializer)
            @Suppress("UNCHECKED_CAST")
            register(Window::class, WindowSerializer as KSerializer<Window<*>>)
            @Suppress("UNCHECKED_CAST")
            register(List::class, ListSerializer(PlainObjectSerializer) as KSerializer<List<*>>)
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

        override fun serialize(encoder: Encoder, value: BeanStreamElement) {
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