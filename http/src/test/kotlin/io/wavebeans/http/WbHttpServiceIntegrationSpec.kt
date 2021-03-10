package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.execution.distributed.Facilitator
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.WavHeader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.stream.Measured
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.TableRegistryImpl
import io.wavebeans.lib.table.toSampleTable
import io.wavebeans.lib.table.toTable
import io.wavebeans.tests.findFreePort
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep

object WbHttpServiceIntegrationSpec : Spek({

    val client by memoized(CachingMode.SCOPE) {
        val c = OkHttpClient.Builder()
            .followRedirects(false)
            .build()
        OkHttp(c)
    }

    val tableRegistry = TableRegistry.default

    describe("Table service") {

        val port = findFreePort()
        val httpService = WbHttpService(serverPort = port, gracePeriodMillis = 100, tableRegistry = tableRegistry)
            .addHandler(JavalinServletHandler.newInstance(tableRegistry))

        beforeGroup { httpService.start() }
        afterGroup { httpService.close() }

        it("should return samples") {

            val tableName = "tableIntegration1"
            val o = 440.sine().toTable(tableName, 1.s)
            val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

            val overseer = SingleThreadedOverseer(listOf(o))
            overseer.eval(44100.0f)
            waitForData<Sample>(tableName)
            overseer.close()

            val result =
                client(Request(Method.GET, "http://localhost:$port/table/$tableName/last?interval=1.ms"))
                    .bodyString()

            assertThat(result.split(Regex("[\\r\\n]+")).filterNot { it.isEmpty() }).all {
                size().isGreaterThanOrEqualTo(1)
                each { it.matches(elementRegex) }
            }
        }

        it("should return custom samples") {

            @Serializable
            data class CustomSample(val sample: Sample) : Measured {
                override fun measure(): Int = 1
            }

            val tableName = "tableIntegration2"
            val o = 440.sine().map { CustomSample(it) }.toTable(tableName, 1.s)
            val elementRegex = elementRegex("\\{\"sample\":-?\\d+\\.\\d+([eE]?-\\d+)?\\}")

            val overseer = SingleThreadedOverseer(listOf(o))
            overseer.eval(44100.0f)
            waitForData<CustomSample>(tableName)
            overseer.close()

            val result =
                client(Request(Method.GET, "http://localhost:$port/table/$tableName/last?interval=1.ms"))
                    .bodyString()

            assertThat(result.split(Regex("[\\r\\n]+")).filterNot { it.isEmpty() }).all {
                size().isGreaterThanOrEqualTo(1)
                each { it.matches(elementRegex) }
            }

        }

    }

    describe("Audio service") {
        val port = findFreePort()
        val httpService = WbHttpService(serverPort = port, gracePeriodMillis = 100, tableRegistry = tableRegistry)
            .addHandler(JavalinServletHandler.newInstance(tableRegistry))

        beforeGroup { httpService.start() }

        afterGroup { httpService.close() }

        it("should stream data for a little bit") {
            val tableName = "audioIntegration1"
            val o = 440.sine().toSampleTable(tableName, 1.s)

            val overseer = SingleThreadedOverseer(listOf(o))
            overseer.eval(44100.0f)
            waitForData<Sample>(tableName)
            val result = client(Request(Method.GET, "http://localhost:$port/audio/$tableName/stream/wav?limit=1ms"))
                .body.stream.readBytes()

            assertThat(result).all {
                size().isGreaterThan(44)
                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                    .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
            }
            overseer.close()
        }
    }

    describe("Running in distributed mode") {

        val httpPort = findFreePort()
        val communicatorPort = findFreePort()
        val facilitatorPort1 = findFreePort()
        val facilitatorPort2 = findFreePort()
        val tableRegistryInner = TableRegistryImpl()
        val httpService = WbHttpService(
            serverPort = httpPort,
            gracePeriodMillis = 100,
            communicatorPort = communicatorPort,
            tableRegistry = tableRegistryInner //just isolated instance
        ).addHandler(JavalinServletHandler.newInstance(tableRegistryInner))


        val tableName = "tableDistributed"

        val o = 440.sine().toSampleTable(tableName, 1.s)

        val facilitator1 = Facilitator(
            communicatorPort = facilitatorPort1,
            threadsNumber = 1,
            podDiscovery = object : PodDiscovery() {},
            onServerShutdownTimeoutMillis = 100
        )

        val facilitator2 = Facilitator(
            communicatorPort = facilitatorPort2,
            threadsNumber = 1,
            podDiscovery = object : PodDiscovery() {},
            onServerShutdownTimeoutMillis = 100
        )

        val overseer = DistributedOverseer(
            listOf(o),
            listOf("127.0.0.1:$facilitatorPort1", "127.0.0.1:$facilitatorPort2"),
            listOf("127.0.0.1:$communicatorPort"),
            partitionsCount = 1
        )

        beforeGroup {
            httpService.start()
            facilitator1.start()
            facilitator2.start()
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


        val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

        it("should return samples") {
            waitForData<Sample>(tableName)
            val result = client(Request(Method.GET, "http://localhost:$httpPort/table/$tableName/last?interval=100ms"))
                .bodyString()
            assertThat(result.split(Regex("[\\r\\n]+")).filterNot { it.isEmpty() }).all {
                size().isGreaterThanOrEqualTo(1)
                each { it.matches(elementRegex) }
            }
        }

    }
})

private fun elementRegex(valueRegex: String) = Regex("[{]\"offset\":\\d+,\"value\":$valueRegex}")

private inline fun <reified T : Any> waitForData(tableName: String) {
    val table = TableRegistry.default.byName<T>(tableName)
    while (table.lastMarker() == null) {
        sleep(0)
    }
}
