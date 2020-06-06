package io.wavebeans.lib.table

import io.wavebeans.lib.*
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.Writer
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.window.window
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Outputs item of any type to table with name [tableName], limiting the maximum data length stored in memory.
 *
 * @param tableName the name of the table to perform the output to. The table then can be located via [TableRegistry]
 * @param maximumDataLength the maximum data length to keep in the table, the rest are going to be removed once they
 * are out of the defined interval. The sample required to be registered with [SampleCountMeasurement]
 *
 * @return the [TableOutput] which is a terminal action for the execuion.
 */
inline fun <reified T : Any> BeanStream<T>.toTable(
        tableName: String,
        maximumDataLength: TimeMeasure = 1.d
): TableOutput<T> = TableOutput(
        this,
        TableOutputParams(
                tableName,
                T::class,
                maximumDataLength
        )
)

/**
 * Outputs [Sample]s to table with name [tableName], limiting the maximum data length and grouping samples into buffers if specified.
 *
 * @param tableName the name of the table to perform the output to. The table then can be located via [TableRegistry]
 * @param maximumDataLength the maximum data length to keep in the table, the rest are going to be removed once they
 * are out of the defined interval. The sample required to be registered with [SampleCountMeasurement]
 * @param sampleArrayBuffer if more than 0 then samples are grouped into [SampleArray] before storing into the table.
 * That allows to reduce the memory footprint and overall improves perfomance for some cases, though by additional delay in the signal.
 *
 * @return the [TableOutput] which is a terminal action for the execuion.
 */
@Suppress("UNCHECKED_CAST")
fun BeanStream<Sample>.toSampleTable(
        tableName: String,
        maximumDataLength: TimeMeasure = 1.d,
        sampleArrayBuffer: Int = 0
): TableOutput<out Any> = TableOutput(
        (if (sampleArrayBuffer > 0) this.window(sampleArrayBuffer).map { sampleArrayOf(it) } else this) as BeanStream<Any>,
        TableOutputParams<Any>(
                tableName,
                if (sampleArrayBuffer > 0) SampleArray::class else Sample::class,
                maximumDataLength
        )
)


@Serializable(with = TableOutputParamsSerializer::class)
class TableOutputParams<T : Any>(
        val tableName: String,
        val tableType: KClass<out T>,
        val maximumDataLength: TimeMeasure,
        val tableDriverFactory: Fn<TableOutputParams<T>, TimeseriesTableDriver<T>> = Fn.wrap {
            InMemoryTimeseriesTableDriver(
                    it.tableName,
                    it.tableType,
                    TimeTableRetentionPolicy(it.maximumDataLength)
            )
        }
) : BeanParams()

object TableOutputParamsSerializer : KSerializer<TableOutputParams<*>> {
    override val descriptor: SerialDescriptor = SerialDescriptor(TableOutputParams::class.jvmName) {
        element("tableName", String.serializer().descriptor)
        element("tableType", String.serializer().descriptor)
        element("maximumDataLength", TimeMeasure.serializer().descriptor)
        element("tableDriverFactory", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): TableOutputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var tableName: String? = null
        var tableType: KClass<*>? = null
        var maximumDataLength: TimeMeasure? = null
        var tableDriverFactory: Fn<TableOutputParams<Any>, TimeseriesTableDriver<Any>>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> tableName = dec.decodeStringElement(descriptor, i)
                1 -> tableType = WaveBeansClassLoader.classForName(dec.decodeStringElement(descriptor, i)).kotlin
                2 -> maximumDataLength = dec.decodeSerializableElement(descriptor, i, TimeMeasure.serializer())
                3 -> tableDriverFactory = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                        as Fn<TableOutputParams<Any>, TimeseriesTableDriver<Any>>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return TableOutputParams(tableName!!, tableType!!, maximumDataLength!!, tableDriverFactory!!)
    }

    override fun serialize(encoder: Encoder, value: TableOutputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, value.tableName)
        structure.encodeStringElement(descriptor, 1, value.tableType.jvmName)
        structure.encodeSerializableElement(descriptor, 2, TimeMeasure.serializer(), value.maximumDataLength)
        structure.encodeSerializableElement(descriptor, 3, FnSerializer, value.tableDriverFactory)
        structure.endStructure(descriptor)
    }
}

/**
 * Outputs item of any type to table with specified name, limiting the maximum data length.
 *
 * The default implementation of the table is [InMemoryTimeseriesTableDriver].
 * You can specifiy a different [TimeseriesTableDriver] via specifying [TableOutputParams.tableDriverFactory] parameter.
 */
class TableOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: TableOutputParams<T>
) : StreamOutput<T>, SinglePartitionBean {

    private val tableDriver: TimeseriesTableDriver<T>

    init {
        val tableRegistry = TableRegistry.default
        val tableName = parameters.tableName

        if (tableRegistry.exists(tableName)) {
            tableDriver = tableRegistry.byName(tableName)
        } else {
            tableDriver = parameters.tableDriverFactory.apply(parameters)
            tableRegistry.register(tableName, tableDriver)
        }
    }

    override fun writer(sampleRate: Float): Writer {
        tableDriver.init(sampleRate)

        val iterator = input.asSequence(sampleRate).iterator()
        var index = tableDriver.lastMarker()?.let { timeToSampleIndexCeil(it, sampleRate) } ?: 0L

        return object : Writer {
            override fun write(): Boolean {
                if (!iterator.hasNext()) return false

                val element = iterator.next()
                val time = SampleCountMeasurement.samplesInObject(element).toLong() * samplesCountToLength(index++, sampleRate, NANOSECONDS)
                tableDriver.put(time.ns, element)
                return true
            }

            override fun close() {
                tableDriver.close()
            }

        }
    }

}