package io.wavebeans.http

import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.wavebeans.lib.table.TableRegistry
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.TimeUnit.*

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
        server?.stop(gracePeriodMillis, timeoutMillis)
        if (communicatorServer?.shutdown()?.awaitTermination(gracePeriodMillis, MILLISECONDS) == false) {
            communicatorServer?.shutdownNow()
        }
        server = null
    }
}

