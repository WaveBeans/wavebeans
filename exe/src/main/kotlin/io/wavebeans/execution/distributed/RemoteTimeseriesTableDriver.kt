package io.wavebeans.execution.distributed

import io.wavebeans.communicator.TableApiClient
import io.wavebeans.execution.TableQuerySerializer
import io.wavebeans.execution.distributed.proto.ProtoObj
import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.table.TableQuery
import io.wavebeans.lib.table.TimeseriesTableDriver
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.jvmName

class RemoteTimeseriesTableDriver<T : Any>(
        override val tableName: String,
        val facilitatorLocation: String,
        override val tableType: KClass<T>
) : TimeseriesTableDriver<T> {

    override val sampleRate: Float
        get() = sampleRateValue[0]
                .let { if (it < 0) throw IllegalStateException("Sample rate value is not initialized yet") else it }

    lateinit var client: TableApiClient

    private val sampleRateValue: FloatArray = FloatArray(1) { Float.NEGATIVE_INFINITY }

    override fun init(sampleRate: Float) {
        sampleRateValue[0] = sampleRate
        client = TableApiClient(tableName, facilitatorLocation)
    }

    override fun reset() {
        client.reset()
    }

    override fun put(time: TimeMeasure, value: T) {
        val protoObj = ProtoObj.wrapIfNeeded(value)
        val kSerializer = SerializableRegistry.find(protoObj::class)
        client.put(time.time, java.util.concurrent.TimeUnit.valueOf(time.timeUnit.toString()), protoObj::class.jvmName, protoObj.asByteArray(kSerializer))
    }

    override fun firstMarker(): TimeMeasure? {
        return client.firstMarker { time, timeUnit -> TimeMeasure(time, TimeUnit.valueOf(timeUnit.toString())) }
    }

    override fun lastMarker(): TimeMeasure? {
        return client.lastMarker { time, timeUnit -> TimeMeasure(time, TimeUnit.valueOf(timeUnit.toString())) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun query(query: TableQuery): Sequence<T> {
        val serializerClass = client.tableElementSerializer()
        val cl = WaveBeansClassLoader.classForName(serializerClass).kotlin
        val kSerializer = (cl.objectInstance ?: cl.createInstance()) as KSerializer<T>
        return client.query(TableQuerySerializer.serialize(query))
                .map { ProtoObj.unwrapIfNeeded(it.asObj(kSerializer)) as T }
    }

    override fun finishStream() = client.finishStream()

    override fun isStreamFinished(): Boolean = client.isStreamFinished()

    override fun close() {
        client.close()
    }

    override fun toString(): String {
        return "RemoteTimeseriesTableDriver(tableName='$tableName', facilitatorLocation='$facilitatorLocation')"
    }
}