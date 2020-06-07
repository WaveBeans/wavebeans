package io.wavebeans.communicator

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.Closeable
import java.util.concurrent.TimeUnit

abstract class AbstractClient(
        val location: String
) : Closeable {

    abstract val onCloseTimeoutMs: Long

    private val server: String
    private val port: Int
    private val channel: ManagedChannel

    init {
        val parts = location.split(":", limit = 2)
        require(parts.size == 2) { "Location `$location` should be in format `server-name-or-ip-address:port`" }
        server = parts[0]
        port = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Port in location `$location` should be numeric")
        require(port in (1..65535)) { "Port in location `$location` should be in range 1..65535" }

        channel = ManagedChannelBuilder.forAddress(server, port).usePlaintext().build()
        this.createClient(channel)
    }

    protected abstract fun createClient(channel: Channel)

    override fun close() {
        if (!channel.shutdown().awaitTermination(onCloseTimeoutMs, TimeUnit.MILLISECONDS)) {
            channel.shutdownNow()
        }
    }
}