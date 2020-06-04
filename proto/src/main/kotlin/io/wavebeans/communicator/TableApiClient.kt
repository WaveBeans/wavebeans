package io.wavebeans.communicator

import com.google.protobuf.ByteString
import io.grpc.Channel
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.valueOf

data class QueryStreamDescriptor(
        val streamId: UUID,
        val serializerClass: String
)

class TableApiClient(
        val tableName: String,
        location: String,
        override val onCloseTimeoutMs: Long = 5000
) : AbstractClient(location) {

    private lateinit var client: TableApiGrpc.TableApiFutureStub
    private lateinit var clientBlocking: TableApiGrpc.TableApiBlockingStub

    override fun createClient(channel: Channel) {
        client = TableApiGrpc.newFutureStub(channel)
        clientBlocking = TableApiGrpc.newBlockingStub(channel)
    }

    fun reset(timeoutMs: Long = 5000) {
        client.reset(
                TableResetRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun put(time: Long, timeUnit: TimeUnit, valueType: String, valueSerialized: ByteArray, timeoutMs: Long = 5000) {
        client.put(
                TablePutRequest.newBuilder()
                        .setTableName(tableName)
                        .setTime(TimeMeasure.newBuilder()
                                .setTime(time)
                                .setTimeUnit(timeUnit.toString())
                                .build()
                        )
                        .setValueType(valueType)
                        .setValueSerialized(ByteString.copyFrom(valueSerialized))
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun <T : kotlin.Any> firstMarker(timeoutMs: Long = 5000, converter: (Long, TimeUnit) -> T): T? {
        val response = client.firstMarker(
                TableMarkerRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, MILLISECONDS)
        return if (!response.isNull)
            converter(response.marker.time, valueOf(response.marker.timeUnit))
        else
            return null
    }

    fun <T : kotlin.Any> lastMarker(timeoutMs: Long = 5000, converter: (Long, TimeUnit) -> T): T? {
        val response = client.lastMarker(
                TableMarkerRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, MILLISECONDS)
        return if (!response.isNull)
            converter(response.marker.time, valueOf(response.marker.timeUnit))
        else
            return null
    }

    fun initQuery(queryAsJson: String, timeoutMs: Long = 5000): QueryStreamDescriptor {
        val response = client.initQuery(
                TableInitQueryRequest.newBuilder()
                        .setTableName(tableName)
                        .setQueryAsJson(queryAsJson)
                        .build()
        ).get(timeoutMs, MILLISECONDS)

        return QueryStreamDescriptor(UUID.fromString(response.streamId), response.serializerClass)
    }

    fun query(streamId: UUID): Sequence<ByteArray> {
        return clientBlocking.query(
                TableQueryRequest.newBuilder()
                        .setStreamId(streamId.toString())
                        .build()
        ).asSequence().map { it.valueSerialized.toByteArray() }
    }

}