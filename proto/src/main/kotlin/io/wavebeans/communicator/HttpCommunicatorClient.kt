package io.wavebeans.communicator

import io.grpc.Channel
import java.util.concurrent.TimeUnit

class HttpCommunicatorClient(
        location: String,
        override val onCloseTimeoutMs: Long = 5000
) : AbstractClient(location) {

    private lateinit var client: HttpCommunicatorGrpc.HttpCommunicatorFutureStub

    override fun createClient(channel: Channel) {
        client = HttpCommunicatorGrpc.newFutureStub(channel)
    }

    fun registerTable(tableName: String, facilitatorLocation: String, sampleRate: Float, timeoutMs: Long = 5000) {
        client.registerTable(
                RegisterTableRequest.newBuilder()
                        .setSampleRate(sampleRate)
                        .setTableName(tableName)
                        .setFacilitatorLocation(facilitatorLocation)
                        .build()
        ).get(timeoutMs, TimeUnit.MILLISECONDS)
    }

    fun unregisterTable(tableName: String, timeoutMs: Long = 5000) {
        client.unregisterTable(
                UnregisterTableRequest.newBuilder()
                        .setTableName(tableName)
                        .build()
        ).get(timeoutMs, TimeUnit.MILLISECONDS)
    }
}