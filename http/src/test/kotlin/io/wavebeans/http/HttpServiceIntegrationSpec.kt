package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.execution.distributed.Facilitator
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.io.WavHeader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.table.TableRegistryImpl
import io.wavebeans.lib.table.toSampleTable
import io.wavebeans.lib.table.toTable
import kotlinx.coroutines.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.URL

@KtorExperimentalAPI
object HttpServiceIntegrationSpec : Spek({


    describe("Table service") {

        val port = 12345
        val httpService = HttpService(serverPort = port, gracePeriodMillis = 100, timeoutMillis = 100)

        beforeGroup { httpService.start() }
        afterGroup { httpService.close() }

        val o = 440.sine().toTable("tableIntegration1", 1.s)
        fun elementRegex(valueRegex: String) = Regex("[{]\"offset\":\\d+,\"value\":$valueRegex}")
        val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

        val overseer = SingleThreadedOverseer(listOf(o))
        overseer.eval(44100.0f)
        afterGroup { overseer.close() }

        val result by memoized {
            runBlocking {
                HttpClient(CIO).use { client ->
                    client.get<String>(URL("http://localhost:$port/table/tableIntegration1/last?interval=1.ms"))
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

        val port = 12346
        val httpService = HttpService(serverPort = port, gracePeriodMillis = 100, timeoutMillis = 100)

        beforeGroup { httpService.start() }
        afterGroup { httpService.close() }

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
                    client.get<ByteArray>(URL("http://localhost:$port/audio/audioIntegration1/stream/wav"))
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

    describe("Running in distributed mode") {

        val httpPort = 12347
        val communicatorPort = 20000
        val facilitatorPort1 = 4000
        val facilitatorPort2 = 4001
        val httpService = HttpService(
                serverPort = httpPort,
                gracePeriodMillis = 100,
                timeoutMillis = 100,
                communicatorPort = communicatorPort,
                tableRegistry = TableRegistryImpl() //just isolated instance
        )


        val o = 440.sine().toSampleTable("tableDistributed", 1.s)

        val facilitator1 = Facilitator(
                communicatorPort = facilitatorPort1,
                threadsNumber = 1,
                podDiscovery = object : PodDiscovery() {},
                onServerShutdownTimeoutMillis = 100
        ).start()

        val facilitator2 = Facilitator(
                communicatorPort = facilitatorPort2,
                threadsNumber = 1,
                podDiscovery = object : PodDiscovery() {},
                onServerShutdownTimeoutMillis = 100
        ).start()

        val overseer = DistributedOverseer(
                listOf(o),
                listOf("127.0.0.1:$facilitatorPort1", "127.0.0.1:$facilitatorPort2"),
                listOf("127.0.0.1:$communicatorPort"),
                partitionsCount = 1
        )

        beforeGroup {
            httpService.start()
            overseer.eval(44100.0f)
        }

        afterGroup {
            overseer.close()
            httpService.close()
            facilitator1.terminate()
            facilitator1.close()
            facilitator2.terminate()
            facilitator2.close()
        }

        val result by memoized {
            runBlocking {
                HttpClient(CIO).use { client ->
                    client.get<String>(URL("http://localhost:$httpPort/table/tableDistributed/last?interval=100ms"))
                }
            }
        }

        fun elementRegex(valueRegex: String) = Regex("[{]\"offset\":\\d+,\"value\":$valueRegex}")
        val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")
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