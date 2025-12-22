package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.kotest.core.spec.style.DescribeSpec
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import java.lang.Thread.sleep

class WbHttpServiceIntegrationSpec : DescribeSpec({

    val client by lazy {
        val c = OkHttpClient.Builder()
            .followRedirects(false)
            .build()
        OkHttp(c)
    }

    var port = -1
    lateinit var httpService: WbHttpService

    beforeTest {
        port = findFreePort()
        httpService = WbHttpService(serverPort = port, gracePeriodMillis = 100)
        httpService.start()
    }

    afterTest { httpService.close() }

    describe("Table service") {

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

        it("should return custom samples with custom serializer") {

            data class AnotherCustomSample(val sample1: Sample) : Measured {
                override fun measure(): Int = 1
            }

            class AnotherCustomSampleSerializer : KSerializer<AnotherCustomSample> {
                override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnotherCustomSample") {
                    element("sample1", Sample.serializer().descriptor)
                }

                override fun deserialize(decoder: Decoder): AnotherCustomSample {
                    throw UnsupportedOperationException("Don't need it")
                }

                override fun serialize(encoder: Encoder, value: AnotherCustomSample) {
                    encoder.encodeStructure(descriptor) {
                        encodeSerializableElement(descriptor, 0, Sample.serializer(), value.sample1)
                    }
                }
            }

            JsonBeanStreamReader.register(AnotherCustomSample::class, AnotherCustomSampleSerializer())

            val tableName = "tableIntegration3"
            val o = 440.sine().map { AnotherCustomSample(it) }.toTable(tableName, 1.s)
            val elementRegex = elementRegex("\\{\"sample1\":-?\\d+\\.\\d+([eE]?-\\d+)?\\}")

            val overseer = SingleThreadedOverseer(listOf(o))
            overseer.eval(44100.0f)
            waitForData<AnotherCustomSample>(tableName)
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
        it("should stream data for a little bit from sample table") {
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

        it("should stream data for a little bit from sample vector table") {
            val tableName = "audioIntegration2"
            val o = 440.sine().toSampleTable(tableName, 1.s, sampleVectorBufferSize = 10)

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

        val tableName = "tableDistributed"
        val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

        val o by lazy {
            440.sine().toSampleTable(tableName, 1.s)
        }

        val httpPort by lazy { findFreePort() }
        val communicatorPort by lazy { findFreePort() }
        val facilitatorPort1 by lazy { findFreePort() }
        val facilitatorPort2 by lazy { findFreePort() }
        val httpService by lazy {
            WbHttpService(
                serverPort = httpPort,
                gracePeriodMillis = 100,
                communicatorPort = communicatorPort,
                tableRegistry = TableRegistryImpl() //just isolated instance
            )
        }

        val facilitator1 by lazy {
            Facilitator(
                threadsNumber = 1,
                communicatorPort = facilitatorPort1,
                onServerShutdownTimeoutMillis = 100,
                podDiscovery = object : PodDiscovery() {},
            )
        }

        val facilitator2 by lazy {
            Facilitator(
                threadsNumber = 1,
                communicatorPort = facilitatorPort2,
                onServerShutdownTimeoutMillis = 100,
                podDiscovery = object : PodDiscovery() {},
            )
        }

        val overseer by lazy {
            DistributedOverseer(
                outputs = listOf(o),
                facilitatorLocations = listOf("127.0.0.1:$facilitatorPort1", "127.0.0.1:$facilitatorPort2"),
                httpLocations = listOf("127.0.0.1:$communicatorPort"),
                partitionsCount = 1
            )
        }

        beforeTest {
            httpService.start()
            facilitator1.start()
            facilitator2.start()
            overseer.eval(44100.0f)
        }

        afterTest {
            overseer.close()
            httpService.close()
            facilitator1.terminate()
            facilitator1.close()
            facilitator2.terminate()
            facilitator2.close()
        }


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
