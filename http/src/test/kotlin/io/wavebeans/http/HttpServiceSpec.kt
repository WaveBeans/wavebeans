package io.wavebeans.http

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.wavebeans.execution.LocalOverseer
import io.wavebeans.lib.asDouble
import io.wavebeans.lib.asInt
import io.wavebeans.lib.asLong
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.lib.table.toTable
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
object HttpServiceSpec : Spek({

    describe("TableService") {

        fun elementRegex(valueRegex: String) = Regex("\\{\"offset\":\\d+,\"value\":$valueRegex}")

        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        engine.application.tableService()
        with(engine) {
            describe("Samples") {
                val o = 440.sine().trim(100).toTable("table1", 1.s)
                val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

                val overseer = LocalOverseer(listOf(o))
                overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                after { overseer.close() }

                it("should return last 100ms") {
                    handleRequest(Get, "/table/table1/last?interval=100ms&sampleRate=44100.0").apply {
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

                val overseer = LocalOverseer(listOf(o))
                overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                after { overseer.close() }

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

                    override val descriptor: SerialDescriptor = SerialDescriptor("B") {
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

                val overseer = LocalOverseer(listOf(o))
                overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                after { overseer.close() }

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

                    val overseer = LocalOverseer(listOf(o))
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    after { overseer.close() }

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

                    val overseer = LocalOverseer(listOf(o))
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    after { overseer.close() }

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

                    val overseer = LocalOverseer(listOf(o))
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    after { overseer.close() }

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

                    val overseer = LocalOverseer(listOf(o))
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    after { overseer.close() }

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

                    val overseer = LocalOverseer(listOf(o))
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                    after { overseer.close() }

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
})

private fun Assert<String>.elements(): Assert<List<String>> = this.prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }