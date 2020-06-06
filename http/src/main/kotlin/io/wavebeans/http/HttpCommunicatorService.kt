package io.wavebeans.http

import io.grpc.stub.StreamObserver
import io.wavebeans.communicator.*
import io.wavebeans.execution.distributed.RemoteTimeseriesTableDriver
import io.wavebeans.lib.table.TableRegistry
import mu.KotlinLogging

class HttpCommunicatorService(
        val tableRegistry: TableRegistry
) {

    companion object {
        private val log = KotlinLogging.logger { }
        fun instance(tableRegistry: TableRegistry): HttpCommunicatorGrpcService =
                HttpCommunicatorGrpcService(HttpCommunicatorService(tableRegistry))
    }

    fun registerTable(tableName: String, facilitatorLocation: String, sampleRate: Float) {
        log.info { "Registering remote table `$tableName` pointed to Facilitator on $facilitatorLocation" }
        val tableDriver = RemoteTimeseriesTableDriver<Any>(tableName, facilitatorLocation, Any::class)
        tableDriver.init(sampleRate)
        tableRegistry.register(tableName, tableDriver)
    }

    fun unregisterTable(tableName: String) {
        val tableDriver = tableRegistry.unregister(tableName)
        tableDriver?.close()
        log.info { "Unegistering remote table `$tableName`: $tableDriver" }
    }
}

class HttpCommunicatorGrpcService(val service: HttpCommunicatorService) : HttpCommunicatorGrpc.HttpCommunicatorImplBase() {

    override fun registerTable(request: RegisterTableRequest, responseObserver: StreamObserver<RegisterTableResponse>) {
        responseObserver.single("registerTable", request) {
            service.registerTable(request.tableName, request.facilitatorLocation, request.sampleRate)
            RegisterTableResponse.newBuilder().build()
        }
    }

    override fun unregisterTable(request: UnregisterTableRequest, responseObserver: StreamObserver<UnregisterTableResponse>) {
        responseObserver.single("unregisterTable", request) {
            service.unregisterTable(request.tableName)
            UnregisterTableResponse.newBuilder().build()
        }
    }
}