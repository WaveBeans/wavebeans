package io.wavebeans.lib.io.table

import io.wavebeans.lib.*
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.Writer
import java.util.concurrent.TimeUnit.NANOSECONDS

fun <T : Any> BeanStream<T>.toTable(
        tableName: String,
        maximumDataLength: TimeMeasure = 1.d
): TableOutput<T> = TableOutput(
        this,
        TableOutputParams(
                tableName,
                InMemoryTimeseriesTableDriver(tableName, TimeTableRetentionPolicy(maximumDataLength))
        )
)

class TableOutputParams<T : Any>(
        val tableName: String,
        val timeseriesTableDriver: TimeseriesTableDriver<T>
) : BeanParams()

class TableOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: TableOutputParams<T>
) : StreamOutput<T>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {
        TableRegistry.instance().register(parameters.tableName, parameters.timeseriesTableDriver)

        val iterator = input.asSequence(sampleRate).iterator()
        var index = 0L

        return object : Writer {
            override fun write(): Boolean {
                if (!iterator.hasNext()) return false

                val element = iterator.next()
                val time = samplesCountToLength(index++, sampleRate, NANOSECONDS)
                parameters.timeseriesTableDriver.put(time.ns, element)
                return true
            }

            override fun close() {
                parameters.timeseriesTableDriver.close()
            }

        }
    }

}