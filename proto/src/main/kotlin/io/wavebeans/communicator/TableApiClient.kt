package io.wavebeans.communicator

import com.google.protobuf.ByteString
import io.grpc.Channel
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.valueOf

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

    fun tableElementSerializer(timeoutMs: Long = 5000): String {
        val response = client.tableElementSerializer(
                TableElementSerializerRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, MILLISECONDS)

        return response.serializerClass
    }

    fun query(queryAsJson: String): Sequence<ByteArray> {
        return clientBlocking.query(
                TableQueryRequest.newBuilder()
                        .setTableName(tableName)
                        .setQueryAsJson(queryAsJson)
                        .build()
        ).asSequence().map { it.valueSerialized.toByteArray() }
    }

    fun finishStream(timeoutMs: Long = 5000) {
        client.finishStream(
                FinishStreamRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun isStreamFinished(timeoutMs: Long = 5000): Boolean {
        return client.isStreamFinished(
                IsStreamFinishedRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, MILLISECONDS).isStreamFinished
    }
}