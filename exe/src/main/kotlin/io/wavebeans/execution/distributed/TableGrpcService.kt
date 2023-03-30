package io.wavebeans.execution.distributed

import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.*
import io.wavebeans.communicator.Any
import io.wavebeans.execution.TableQuerySerializer
import io.wavebeans.execution.distributed.proto.ProtoObj
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.table.TableRegistry
import kotlinx.serialization.KSerializer
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

class TableGrpcService(
        val tableRegistry: TableRegistry
) {

    companion object {
        private val log = KotlinLogging.logger { }
        fun instance(tableRegistry: TableRegistry): TableApiGrpcService =
                TableApiGrpcService(TableGrpcService(tableRegistry))
    }

    fun firstMarker(tableName: String): io.wavebeans.lib.TimeMeasure? =
            tableRegistry.byName<kotlin.Any>(tableName).firstMarker()
                    .also { log.trace { "Getting firstMarker for $tableName. Returned $it" } }

    fun lastMarker(tableName: String): io.wavebeans.lib.TimeMeasure? =
            tableRegistry.byName<kotlin.Any>(tableName).lastMarker()
                    .also { log.trace { "Getting firstMarker for $tableName. Returned $it" } }

    fun put(tableName: String, marker: io.wavebeans.lib.TimeMeasure, valueType: String, valueSerialized: ByteArray) {
        val table = tableRegistry.byName<kotlin.Any>(tableName)
        val clazz = WaveBeansClassLoader.classForName(valueType).kotlin
        val kSerializer = SerializableRegistry.find(clazz)
        val obj = ProtoObj.unwrapIfNeeded(valueSerialized.asObj(kSerializer))
                ?: throw IllegalStateException("Trying to put null value into $tableName " +
                        "[marker=$marker, valueType=$valueType, valueSerialized=$valueSerialized]")
        table.put(marker, obj)
    }

    fun reset(tableName: String) {
        tableRegistry.byName<kotlin.Any>(tableName).reset()
    }

    fun tableElementSerializer(tableName: String): String {
        val table = tableRegistry.byName<kotlin.Any>(tableName)
        val kSerializer = ProtoObj.serializerForMaybeWrappedObj(table.tableType)

        return kSerializer::class.jvmName
    }

    fun query(tableName: String, queryAsJson: String): Sequence<ByteArray> {
        val table = tableRegistry.byName<kotlin.Any>(tableName)
        val tableQuery = TableQuerySerializer.deserialize(queryAsJson)

        @Suppress("UNCHECKED_CAST")
        val kSerializer = ProtoObj.serializerForMaybeWrappedObj(table.tableType) as KSerializer<kotlin.Any>

        return table.query(tableQuery).map {
            val e = ProtoObj.wrapIfNeeded(it)
            e.asByteArray(kSerializer)
        }
    }

    fun finishStream(tableName: String) {
        val table = tableRegistry.byName<kotlin.Any>(tableName)
        table.finishStream()
    }

    fun isStreamFinished(tableName: String): Boolean {
        val table = tableRegistry.byName<kotlin.Any>(tableName)
        return table.isStreamFinished()
    }
}

class TableApiGrpcService(
        val tableGrpcService: TableGrpcService
) : TableApiGrpc.TableApiImplBase() {

    override fun firstMarker(request: TableMarkerRequest, responseObserver: StreamObserver<TableMarkerResponse>) {
        responseObserver.single("TableApiGrpcService.firstMarker", request) {
            val time = tableGrpcService.firstMarker(request.tableName)
            respondMarker(time)
        }
    }

    override fun lastMarker(request: TableMarkerRequest, responseObserver: StreamObserver<TableMarkerResponse>) {
        responseObserver.single("TableApiGrpcService.lastMarker", request) {
            val time = tableGrpcService.lastMarker(request.tableName)
            respondMarker(time)
        }
    }

    private fun respondMarker(time: io.wavebeans.lib.TimeMeasure?) =
            TableMarkerResponse.newBuilder().apply {
                if (time != null) {
                    this.isNull = false
                    this.marker = TimeMeasure.newBuilder()
                            .setTime(time.time)
                            .setTimeUnit(time.timeUnit.toString())
                            .build()

                } else {
                    this.isNull = true
                }
            }.build()


    override fun put(request: TablePutRequest, responseObserver: StreamObserver<TablePutResponse>) {
        responseObserver.single("TableApiGrpcService.put", request) {
            tableGrpcService.put(
                    request.tableName,
                    io.wavebeans.lib.TimeMeasure(request.time.time, io.wavebeans.lib.TimeUnit.valueOf(request.time.timeUnit)),
                    request.valueType,
                    request.valueSerialized.toByteArray()
            )
            TablePutResponse.getDefaultInstance()
        }
    }

    override fun reset(request: TableResetRequest, responseObserver: StreamObserver<TableResetResponse>) {
        responseObserver.single("TableApiGrpcService.reset", request) {
            tableGrpcService.reset(request.tableName)
            TableResetResponse.getDefaultInstance()
        }
    }

    override fun tableElementSerializer(request: TableElementSerializerRequest, responseObserver: StreamObserver<TableElementSerializerResponse>) {
        responseObserver.single("TableApiGrpcService.tableType", request) {
            val kSerializer = tableGrpcService.tableElementSerializer(request.tableName)
            TableElementSerializerResponse.newBuilder()
                    .setSerializerClass(kSerializer)
                    .build()
        }
    }

    override fun query(request: TableQueryRequest, responseObserver: StreamObserver<Any>) {
        responseObserver.sequence("TableApiGrpcService.query", request) {
            tableGrpcService.query(request.tableName, request.queryAsJson)
                    .map {
                        Any.newBuilder()
                                .setValueSerialized(ByteString.copyFrom(it))
                                .build()
                    }
        }
    }

    override fun finishStream(request: FinishStreamRequest, responseObserver: StreamObserver<FinishStreamResponse>) {
        responseObserver.single("TableApiGrpcService.finishStream", request) {
            tableGrpcService.finishStream(request.tableName)
            FinishStreamResponse.getDefaultInstance()
        }
    }

    override fun isStreamFinished(request: IsStreamFinishedRequest, responseObserver: StreamObserver<IsStreamFinishedResponse>) {
        responseObserver.single("TableApiGrpcService.finishStream", request) {
            IsStreamFinishedResponse.newBuilder()
                    .setIsStreamFinished(tableGrpcService.isStreamFinished(request.tableName))
                    .build()
        }
    }
}