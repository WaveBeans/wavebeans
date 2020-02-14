package io.wavebeans.http

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.wavebeans.execution.LocalOverseer
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.table.toTable
import kotlinx.serialization.Serializable
import java.io.Closeable

class HttpService(
        private val serverPort: Int = 8080
) : Closeable {

    lateinit var server: ApplicationEngine

    fun start(wait: Boolean = false) {
        val env = applicationEngineEnvironment {
            module {
                tableService()
            }
            connector {
                host = "0.0.0.0"
                port = serverPort
            }
        }
        server = embeddedServer(Netty, env).start(wait)
    }

    override fun close() {
        server.stop(5000, 5000)
    }
}

