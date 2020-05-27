package io.wavebeans.http

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.Closeable

class HttpService(
        private val serverPort: Int = 8080,
        private val gracePeriodMillis: Long = 5000,
        private val timeoutMillis: Long = 5000
) : Closeable {

    lateinit var server: ApplicationEngine

    fun start(wait: Boolean = false): HttpService {
        val env = applicationEngineEnvironment {
            module {
                tableService()
                audioService()
            }
            connector {
                host = "0.0.0.0"
                port = serverPort
            }
        }
        server = embeddedServer(Netty, env).start(wait)
        return this
    }

    override fun close() {
        server.stop(gracePeriodMillis, timeoutMillis)
    }
}

