package io.wavebeans.http

import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.wavebeans.lib.table.TableRegistry
import mu.KotlinLogging
import org.slf4j.event.Level
import java.io.Closeable
import java.util.concurrent.TimeUnit.MILLISECONDS

class HttpService(
        private val serverPort: Int = 8080,
        private val communicatorPort: Int? = null,
        private val gracePeriodMillis: Long = 5000,
        private val timeoutMillis: Long = 5000,
        private val tableRegistry: TableRegistry = TableRegistry.default
) : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private var server: ApplicationEngine? = null
    private var communicatorServer: Server? = null

    fun start(wait: Boolean = false): HttpService {
        if (server != null) throw IllegalStateException("Can't start the server, it is already started")
        log.info { "Starting HTTP Service on port $serverPort" }
        val env = applicationEngineEnvironment {
            module {
                tableService(tableRegistry)
                audioService(tableRegistry)
                install(CallLogging) {
                    level = Level.INFO
                }
                install(CORS) {
                    allowNonSimpleContentTypes = true
                    anyHost()
                }
            }
            connector {
                host = "0.0.0.0"
                port = serverPort
            }
        }

        communicatorServer = communicatorPort?.let {
            log.info { "Starting HTTP Communicator on port $it" }
            ServerBuilder.forPort(it)
                    .addService(HttpCommunicatorService.instance(tableRegistry))
                    .build()
                    .start()
        }

        server = embeddedServer(Netty, env).start(wait)

        return this
    }

    override fun close() {
        server?.let {
            log.info { "Stopping HTTP Service on port $serverPort..." }
            it.stop(gracePeriodMillis, timeoutMillis)
        }
        communicatorServer?.let {
            log.info { "Stopping HTTP Communicator Service on port $communicatorPort..." }
            if (!it.shutdown().awaitTermination(gracePeriodMillis, MILLISECONDS)) {
                it.shutdownNow()
            }
        }
        server = null
        communicatorServer = null
    }
}
