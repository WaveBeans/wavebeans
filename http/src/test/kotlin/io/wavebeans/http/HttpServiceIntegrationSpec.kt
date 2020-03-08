package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isGreaterThan
import assertk.assertions.matches
import assertk.assertions.size
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import io.wavebeans.execution.LocalOverseer
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.table.toTable
import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.URL

@KtorExperimentalAPI
object HttpServiceIntegrationSpec : Spek({
    fun elementRegex(valueRegex: String) = Regex("\\{\"offset\":\\d+,\"value\":$valueRegex}")

    describe("Table service") {

        val o = 440.sine().toTable("tableIntegration1", 1.s)
        val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

        val overseer = LocalOverseer(listOf(o))
        overseer.eval(44100.0f)
        after { overseer.close() }

        val result = HttpService(serverPort = 12345).use {
            it.start()

            runBlocking {
                HttpClient(CIO).use { client ->
                    val response = client.get<String>(URL("http://localhost:12345/table/tableIntegration1/last?interval=1.ms"))
                    response
                }
            }
        }

        it("should return samples") {
            assertThat(
                    result.split(Regex("[\\r\\n]+"))
                            .filterNot { it.isEmpty() }
            ).all {
                size().isGreaterThan(1)
                each { it.matches(elementRegex) }
            }
        }
    }
})