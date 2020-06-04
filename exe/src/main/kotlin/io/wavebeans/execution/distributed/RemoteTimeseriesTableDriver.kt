package io.wavebeans.execution.distributed

import io.wavebeans.communicator.TableApiClient
import io.wavebeans.execution.TableQuerySerializer
import io.wavebeans.execution.distributed.proto.ProtoObj
import io.wavebeans.lib.TimeMeasure
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

    lateinit var client: TableApiClient

    override fun init() {
        client = TableApiClient(tableName, facilitatorLocation)
    }

    override fun reset() {
        client.reset()
    }

    override fun put(time: TimeMeasure, value: T) {
        val protoObj = ProtoObj.wrapIfNeeded(value)
        val kSerializer = SerializableRegistry.find(protoObj::class)
        client.put(time.time, time.timeUnit, protoObj::class.jvmName, protoObj.asByteArray(kSerializer))
    }

    override fun firstMarker(): TimeMeasure? {
        return client.firstMarker { time, timeUnit -> TimeMeasure(time, timeUnit) }
    }

    override fun lastMarker(): TimeMeasure? {
        return client.lastMarker { time, timeUnit -> TimeMeasure(time, timeUnit) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun query(query: TableQuery): Sequence<T> {
        val (streamId, serializerClass) = client.initQuery(TableQuerySerializer.serialize(query))
        val cl = WaveBeansClassLoader.classForName(serializerClass).kotlin
        val kSerializer = (cl.objectInstance ?: cl.createInstance()) as KSerializer<T>
        return client.query(streamId).map { ProtoObj.unwrapIfNeeded(it.asObj(kSerializer)) as T }
    }

    override fun close() {
        client.close()
    }

    override fun toString(): String {
        return "RemoteTimeseriesTableDriver(tableName='$tableName', facilitatorLocation='$facilitatorLocation')"
    }
}