package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.io.FunctionStreamOutputParams
import io.wavebeans.lib.io.WriteFunctionArgument
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.reflect.KClass

/**
 * Serializer for [FunctionStreamOutputParams].
 */
@Suppress("UNCHECKED_CAST")
object FunctionStreamOutputParamsSerializer : KSerializer<FunctionStreamOutputParams<*>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(FunctionStreamOutputParams::class.className()) {
            element("sampleClazz", String.serializer().descriptor)
            element("scope", ExecutionScope.serializer().descriptor)
            element("writeFunction", String.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): FunctionStreamOutputParams<*> {
        return decoder.decodeStructure(descriptor) {
            lateinit var sampleClazz: KClass<Any>
            lateinit var scope: ExecutionScope
            lateinit var writeFunction: String
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> sampleClazz =
                        WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i)) as KClass<Any>

                    1 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())

                    2 -> writeFunction = decodeStringElement(descriptor, i)

                    else -> throw SerializationException("Unknown index $i")
                }
            }
            FunctionStreamOutputParams(sampleClazz, scope, lambdaWrapper.deserialize2<ExecutionScope, WriteFunctionArgument<Any>, Boolean>(writeFunction))
        }
    }

    override fun serialize(encoder: Encoder, value: FunctionStreamOutputParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, String.serializer(), value.sampleClazz.className())
            encodeSerializableElement(descriptor, 1, ExecutionScope.serializer(), value.scope)
            encodeStringElement(descriptor, 2, lambdaWrapper.serialize(value.writeFunction))
        }
    }
}
