package io.wavebeans.http

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.server.testing.*
import io.ktor.util.*
import io.wavebeans.execution.MultiThreadedOverseer
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.lib.*
import io.wavebeans.lib.io.WavHeader
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.toSampleTable
import io.wavebeans.lib.table.toTable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
object HttpServiceSpec : Spek({

    describe("TableService") {

        fun elementRegex(valueRegex: String) = Regex("\\{\"offset\":\\d+,\"value\":$valueRegex}")

        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        engine.application.tableService(TableRegistry.default)
        with(engine) {
            describe("Samples") {
                val o = 440.sine().trim(100).toTable("table1", 1.s)
                val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }
                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    handleRequest(Get, "/table/table1/last?interval=100ms").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
                it("should return time range") {
                    handleRequest(Get, "/table/table1/timeRange?from=0ms&to=100ms").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
                it("should return 404 as table is not found") {
                    handleRequest(Get, "/table/non-existing-table/timeRange?from=0ms&to=100ms").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.NotFound)
                    }
                    handleRequest(Get, "/table/non-existing-table/last?interval=100ms").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.NotFound)
                    }
                }
                it("should return 400 if parameter is incorrect") {
                    handleRequest(Get, "/table/table1/timeRange?from=100&to=200").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.BadRequest)
                    }
                    handleRequest(Get, "/table/table1/last?interval=100").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.BadRequest)
                    }
                }
            }

            describe("Custom class") {

                @Serializable
                data class A(val v: Long)

                SampleCountMeasurement.registerType(A::class) { 1 }

                val o = 440.sine().map { A(it.asLong()) }.trim(100).toTable("table2", 1.s)
                val elementRegex = elementRegex("\\{\"v\":-?\\d+}")

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }

                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    handleRequest(Get, "/table/table2/last?interval=100ms&sampleRate=44100.0").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
                it("should return time range") {
                    handleRequest(Get, "/table/table2/timeRange?from=0ms&to=100ms&sampleRate=44100.0").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }

                }
            }


            describe("External serializer") {

                data class B(val v: String)

                class BSerializer : KSerializer<B> {

                    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("B") {
                        element("v", String.serializer().descriptor)
                    }

                    override fun deserialize(decoder: Decoder): B = throw UnsupportedOperationException("Don't need it")

                    override fun serialize(encoder: Encoder, value: B) {
                        val s = encoder.beginStructure(descriptor)
                        s.encodeStringElement(descriptor, 0, value.v)
                        s.endStructure(descriptor)
                    }
                }
                JsonBeanStreamReader.register(B::class, BSerializer())
                SampleCountMeasurement.registerType(B::class) { 1 }

                val o = 440.sine().trim(10).map { B(it.asLong().toString(16)) }.toTable("b")

                val elementRegex = elementRegex("\\{\"v\":\"-?[0-9a-fA-F]+\"}")

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }
                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    handleRequest(Get, "/table/b/last?interval=1ms&sampleRate=44100.0").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
            }

            describe("Built-in types") {
                describe("FftSample") {
                    val o = 440.sine().trim(100).window(401).fft(512).toTable("fft")
                    val elementRegex = elementRegex("\\{" +
                            "\"index\":\\d+," +
                            "\"binCount\":\\d+," +
                            "\"samplesCount\":\\d+," +
                            "\"sampleRate\":[\\d\\.]+," +
                            "\"magnitude\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"phase\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"frequency\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"time\":\\d+" +
                            "\\}")

                    val overseer = SingleThreadedOverseer(listOf(o))
                    beforeGroup {
                        overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    }
                    afterGroup { overseer.close() }

                    it("should return last 100ms") {
                        handleRequest(Get, "/table/fft/last?interval=100ms").apply {
                            assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                            assertThat(response.content).isNotNull().all {
                                isNotEmpty()
                                elements().each { it.matches(elementRegex) }
                            }
                        }
                    }
                }
                describe("Window<Sample>") {
                    val o = 440.sine().trim(100).window(401).toTable("windowSample")
                    val elementRegex = elementRegex("\\{" +
                            "\"size\":\\d+," +
                            "\"step\":\\d+," +
                            "\"elements\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"sampleType\":\"[\\w\\.$\\_]+\"" +
                            "\\}")

                    val overseer = SingleThreadedOverseer(listOf(o))
                    beforeGroup {
                        overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    }
                    afterGroup { overseer.close() }

                    it("should return last 100ms") {
                        handleRequest(Get, "/table/windowSample/last?interval=100ms").apply {
                            assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                            assertThat(response.content).isNotNull().all {
                                isNotEmpty()
                                elements().each { it.matches(elementRegex) }
                            }
                        }
                    }
                }
                describe("List<Double>") {
                    val o = 440.sine().trim(100).window(401).map { it.elements.map { it.asDouble() } }.toTable("listDouble")
                    val elementRegex = elementRegex("\\[[-eE\\d\\.\\,]+\\]")

                    val overseer = SingleThreadedOverseer(listOf(o))
                    beforeGroup {
                        overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    }
                    afterGroup { overseer.close() }

                    it("should return last 100ms") {
                        handleRequest(Get, "/table/listDouble/last?interval=100ms").apply {
                            assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                            assertThat(response.content).isNotNull().all {
                                isNotEmpty()
                                elements().each { it.matches(elementRegex) }
                            }
                        }
                    }
                }
                describe("List<Int>") {
                    val o = 440.sine().trim(100).window(401).map { it.elements.map { it.asInt() } }.toTable("listInt")
                    val elementRegex = elementRegex("\\[[-\\d\\,]+\\]")

                    val overseer = SingleThreadedOverseer(listOf(o))
                    beforeGroup {
                        overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    }
                    afterGroup { overseer.close() }

                    it("should return last 100ms") {
                        handleRequest(Get, "/table/listInt/last?interval=100ms").apply {
                            assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                            assertThat(response.content).isNotNull().all {
                                isNotEmpty()
                                elements().each { it.matches(elementRegex) }
                            }
                        }
                    }
                }
                describe("Window<A>") {

                    @Serializable
                    data class A(val v: Long)
                    SampleCountMeasurement.registerType(A::class) { 1 }

                    val o = 440.sine().map { A(it.asLong()) }.trim(100).window(401) { A(0) }.toTable("windowA")
                    val elementRegex = elementRegex("\\{" +
                            "\"size\":\\d+," +
                            "\"step\":\\d+," +
                            "\"elements\":\\[(\\{\"v\":[-\\d]+},?)+\\]," +
                            "\"sampleType\":\"[\\w\\.$\\_]+\"" +
                            "\\}")

                    val overseer = SingleThreadedOverseer(listOf(o))
                    beforeGroup {
                        overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    }
                    afterGroup { overseer.close() }

                    it("should return last 100ms") {
                        handleRequest(Get, "/table/windowA/last?interval=100ms").apply {
                            assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                            assertThat(response.content).isNotNull().all {
                                isNotEmpty()
                                elements().each { it.matches(elementRegex) }
                            }
                        }
                    }
                }
            }
        }
    }

    describe("AudioService") {
        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        engine.application.audioService(TableRegistry.default)

        describe("Streaming Wav") {
            val o = 440.sine().trim(2000).toSampleTable("mySampleTable", 1.s)
            val o2 = 440.sine().trim(2000).toSampleTable("mySampleVectorTable", 1.s, 441)
            val overseer = MultiThreadedOverseer(listOf(o, o2), 2, 1)

            beforeGroup { overseer.eval(44100.0f).all { it.get().finished } }

            afterGroup { overseer.close() }

            it("should return 404 for non-existing table") {
                engine.handleRequest(Get, "/audio/nonExistingTable/stream/wav").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.NotFound)
                }
            }
            it("should return stream for existing table in 8 bit format") {
                engine.handleRequest(Get, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=8").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                    assertThat(response.headers)
                            .prop("Content-Type") { it["Content-Type"] }
                            .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(response.byteContent)
                            .isNotNull().all {
                                size().isEqualTo(44 + 1 * 4410)
                                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                        .isEqualTo(WavHeader(BitDepth.BIT_8, 44100.0f, 1, Int.MAX_VALUE).header())
                            }
                }
            }
            it("should return stream for existing table in 16 bit format") {
                engine.handleRequest(Get, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=16").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                    assertThat(response.headers)
                            .prop("Content-Type") { it["Content-Type"] }
                            .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(response.byteContent)
                            .isNotNull().all {
                                size().isEqualTo(44 + 2 * 4410)
                                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                        .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
                            }
                }
            }
            it("should return stream for existing table in 24 bit format") {
                engine.handleRequest(Get, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=24").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                    assertThat(response.headers)
                            .prop("Content-Type") { it["Content-Type"] }
                            .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(response.byteContent)
                            .isNotNull().all {
                                size().isEqualTo(44 + 3 * 4410)
                                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                        .isEqualTo(WavHeader(BitDepth.BIT_24, 44100.0f, 1, Int.MAX_VALUE).header())
                            }
                }
            }
            it("should return stream for existing table in 32 bit format") {
                engine.handleRequest(Get, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=32").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                    assertThat(response.headers)
                            .prop("Content-Type") { it["Content-Type"] }
                            .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(response.byteContent)
                            .isNotNull().all {
                                size().isEqualTo(44 + 4 * 4410)
                                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                        .isEqualTo(WavHeader(BitDepth.BIT_32, 44100.0f, 1, Int.MAX_VALUE).header())
                            }
                }
            }
            it("should return 400 for unknown bit depth") {
                engine.handleRequest(Get, "/audio/nonExistingTable/stream/wav?limit=100ms&offset=1s&bitDepth=42").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.BadRequest)
                }
            }
            it("should return stream for existing sample vector table") {
                engine.handleRequest(Get, "/audio/mySampleVectorTable/stream/wav?limit=100ms&offset=1s").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                    assertThat(response.headers)
                            .prop("Content-Type") { it["Content-Type"] }
                            .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(response.byteContent)
                            .isNotNull().all {
                                size().isEqualTo(44 + 2 * 4410)
                                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                        .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
                            }
                }
            }
            it("should return stream limited with 200ms") {
                engine.handleRequest(Get, "/audio/mySampleTable/stream/wav?limit=200ms&offset=1s").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                    assertThat(response.headers)
                            .prop("Content-Type") { it["Content-Type"] }
                            .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(response.byteContent)
                            .isNotNull().all {
                                size().isEqualTo(44 + 2 * 8820)
                                prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                        .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
                            }
                }
            }
            it("should return 400 for unrecognized limit") {
                engine.handleRequest(Get, "/audio/mySampleTable/stream/wav?limit=100megaseconds").apply {
                    assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.BadRequest)
                }
            }
        }
    }
})

private fun Assert<String>.elements(): Assert<List<String>> = this.prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }