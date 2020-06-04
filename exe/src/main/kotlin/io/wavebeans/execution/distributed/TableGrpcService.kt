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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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

    private val querySequences = ConcurrentHashMap<UUID, Pair<KSerializer<kotlin.Any>, Sequence<kotlin.Any>>>()

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

    fun initQuery(tableName: String, queryAsJson: String): QueryStreamDescriptor {
        val tableQuery = TableQuerySerializer.deserialize(queryAsJson)
        val table = tableRegistry.byName<kotlin.Any>(tableName)
        val kSerializer = ProtoObj.serializerForMaybeWrappedObj(table.tableType)
        val streamId = UUID.randomUUID()
        @Suppress("UNCHECKED_CAST")
        querySequences[streamId] = Pair(kSerializer as KSerializer<kotlin.Any>, table.query(tableQuery))

        return QueryStreamDescriptor(streamId, kSerializer::class.jvmName)
    }

    fun query(streamId: UUID): Sequence<ByteArray> {
        val sequenceDesc = (querySequences[streamId]
                ?: throw IllegalArgumentException("Query $streamId is not initialized"))

        return sequenceDesc.second.map {
            val e = ProtoObj.wrapIfNeeded(it)
            e.asByteArray(sequenceDesc.first)
        }
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
                    io.wavebeans.lib.TimeMeasure(request.time.time, TimeUnit.valueOf(request.time.timeUnit)),
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

    override fun initQuery(request: TableInitQueryRequest, responseObserver: StreamObserver<TableInitQueryResponse>) {
        responseObserver.single("TableApiGrpcService.initQuery", request) {
            val descriptor = tableGrpcService.initQuery(request.tableName, request.queryAsJson)
            TableInitQueryResponse.newBuilder()
                    .setStreamId(descriptor.streamId.toString())
                    .setSerializerClass(descriptor.serializerClass)
                    .build()
        }
    }

    override fun query(request: TableQueryRequest, responseObserver: StreamObserver<Any>) {
        responseObserver.sequence("TableApiGrpcService.query", request) {
            tableGrpcService.query(UUID.fromString(request.streamId))
                    .map {
                        Any.newBuilder()
                                .setValueSerialized(ByteString.copyFrom(it))
                                .build()
                    }
        }
    }
}