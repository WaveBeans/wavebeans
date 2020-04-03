package io.wavebeans.lib.table

import io.wavebeans.lib.*
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.Writer
import io.wavebeans.lib.stream.SampleCountMeasurement
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit.NANOSECONDS

fun <T : Any> BeanStream<T>.toTable(
        tableName: String,
        maximumDataLength: TimeMeasure = 1.d
): TableOutput<T> = TableOutput(
        this,
        TableOutputParams(
                tableName,
                maximumDataLength
        )
)

@Serializable
class TableOutputParams(
        val tableName: String,
        val maximumDataLength: TimeMeasure
) : BeanParams()

class TableOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: TableOutputParams
) : StreamOutput<T>, SinglePartitionBean {

    private val tableDriver: TimeseriesTableDriver<T>

    init {
        val tableRegistry = TableRegistry.instance()
        val tableName = parameters.tableName

        if (tableRegistry.exists(tableName)) {
            tableDriver = tableRegistry.byName(tableName)
        } else {
            tableDriver = InMemoryTimeseriesTableDriver(
                    tableName,
                    TimeTableRetentionPolicy(parameters.maximumDataLength)
            )
            tableRegistry.register(tableName, tableDriver)
        }
    }

    override fun writer(sampleRate: Float): Writer {
        tableDriver.init()

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