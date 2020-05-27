package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.io.WavHeader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.table.toSampleTable
import io.wavebeans.lib.table.toTable
import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.URL

@KtorExperimentalAPI
object HttpServiceIntegrationSpec : Spek({

    val httpService = HttpService(serverPort = 12345, gracePeriodMillis = 100, timeoutMillis = 100)

    beforeGroup { httpService.start() }
    afterGroup { httpService.close() }

    describe("Table service") {

        val o = 440.sine().toTable("tableIntegration1", 1.s)
        fun elementRegex(valueRegex: String) = Regex("[{]\"offset\":\\d+,\"value\":$valueRegex}")
        val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

        val overseer = SingleThreadedOverseer(listOf(o))
        overseer.eval(44100.0f)
        afterGroup { overseer.close() }

        val result by memoized {
            runBlocking {
                HttpClient(CIO).use { client ->
                    client.get<String>(URL("http://localhost:12345/table/tableIntegration1/last?interval=1.ms"))
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

    describe("Audio service") {

        val o = 440.sine().toSampleTable("audioIntegration1", 1.s)

        val overseer = SingleThreadedOverseer(listOf(o))
        overseer.eval(44100.0f)
        afterGroup { overseer.close() }

        val result by memoized {
            runBlocking {
                HttpClient(CIO) {
                    install(HttpTimeout) {
                        requestTimeoutMillis = 1000
                    }
                }.use { client ->
                    client.get<ByteArray>(URL("http://localhost:12345/audio/audioIntegration1/stream/wav"))
                }
            }
        }

        it("should stream data for a little bit") {
            assertThat(result).all {
                size().isGreaterThan(44)
                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                        .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE))
            }
        }
    }
})