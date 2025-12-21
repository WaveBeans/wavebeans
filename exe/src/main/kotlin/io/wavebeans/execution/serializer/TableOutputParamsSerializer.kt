@file:Suppress("UNCHECKED_CAST")

package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.table.TableOutputParams
import io.wavebeans.lib.table.TimeseriesTableDriver
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
 * Serializer for [TableOutputParams].
 */
object TableOutputParamsSerializer : KSerializer<TableOutputParams<*>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TableOutputParams::class.className()) {
        element("tableName", String.serializer().descriptor)
        element("tableType", String.serializer().descriptor)
        element("maximumDataLength", TimeMeasure.serializer().descriptor)
        element("automaticCleanupEnabled", Boolean.serializer().descriptor)
        element("tableDriverFactory", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): TableOutputParams<*> {
        return decoder.decodeStructure(descriptor) {
            lateinit var tableName: String
            lateinit var tableType: KClass<*>
            lateinit var maximumDataLength: TimeMeasure
            var automaticCleanupEnabled = true
            lateinit var tableDriverFactory: Fn<TableOutputParams<Any>, TimeseriesTableDriver<Any>>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> tableName = decodeStringElement(descriptor, i)
                    1 -> tableType = WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i))
                    2 -> maximumDataLength = decodeSerializableElement(descriptor, i, TimeMeasure.serializer())
                    3 -> automaticCleanupEnabled = decodeBooleanElement(descriptor, i)
                    4 -> tableDriverFactory = decodeSerializableElement(descriptor, i, FnSerializer)
                            as Fn<TableOutputParams<Any>, TimeseriesTableDriver<Any>>

                    else -> throw SerializationException("Unknown index $i")
                }
            }
            TableOutputParams(
                tableName,
                tableType as KClass<Any>,
                maximumDataLength,
                automaticCleanupEnabled,
            ) { tableDriverFactory.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: TableOutputParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.tableName)
            encodeStringElement(descriptor, 1, value.tableType.className())
            encodeSerializableElement(descriptor, 2, TimeMeasure.serializer(), value.maximumDataLength)
            encodeSerializableElement(descriptor, 3, Boolean.serializer(), value.automaticCleanupEnabled)
            encodeSerializableElement(descriptor, 4, FnSerializer, wrap(value.tableDriverFactory))
        }
    }
}
