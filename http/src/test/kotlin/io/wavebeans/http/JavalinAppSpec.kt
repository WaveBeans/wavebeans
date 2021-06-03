package io.wavebeans.http

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.javalin.Javalin
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
import io.wavebeans.tests.findFreePort
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object JavalinAppSpec : Spek({

    val javalin by memoized(SCOPE) { Javalin.create().start(findFreePort()) }

    val app by memoized(SCOPE) {
        DefaultJavalinApp(
            listOf(
                { it.tableService(TableRegistry.default) },
                { it.audioService(TableRegistry.default) },
            )
        )
    }

    beforeGroup { app.setUp(javalin) }
    afterGroup { javalin.stop() }

    describe("TableService") {

        fun elementRegex(valueRegex: String) = Regex("\\{\"offset\":\\d+,\"value\":$valueRegex}")

        describe("Samples") {
            val o = 440.sine().trim(100).toTable("table1", 1.s)
            val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

            val overseer = SingleThreadedOverseer(listOf(o))
            beforeGroup {
                overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
            }
            afterGroup { overseer.close() }

            it("should return last 100ms") {
                javalin.handleRequest(GET, "/table/table1/last?interval=100ms").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(bodyString()).isNotNull().all {
                        isNotEmpty()
                        elements().each { it.matches(elementRegex) }
                    }
                }
            }
            it("should return time range") {
                javalin.handleRequest(GET, "/table/table1/timeRange?from=0ms&to=100ms").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(bodyString()).isNotNull().all {
                        isNotEmpty()
                        elements().each { it.matches(elementRegex) }
                    }
                }
            }
            it("should return 404 as table is not found") {
                javalin.handleRequest(GET, "/table/non-existing-table/timeRange?from=0ms&to=100ms").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.NOT_FOUND)
                }
                javalin.handleRequest(GET, "/table/non-existing-table/last?interval=100ms").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.NOT_FOUND)
                }
            }
            it("should return 400 if parameter is incorrect") {
                javalin.handleRequest(GET, "/table/table1/timeRange?from=100&to=200").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.BAD_REQUEST)
                }
                javalin.handleRequest(GET, "/table/table1/last?interval=100").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.BAD_REQUEST)
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
                javalin.handleRequest(GET, "/table/table2/last?interval=100ms&sampleRate=44100.0").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(bodyString()).isNotNull().all {
                        isNotEmpty()
                        elements().each { it.matches(elementRegex) }
                    }
                }
            }
            it("should return time range") {
                javalin.handleRequest(GET, "/table/table2/timeRange?from=0ms&to=100ms&sampleRate=44100.0").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(bodyString()).isNotNull().all {
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
                javalin.handleRequest(GET, "/table/b/last?interval=1ms&sampleRate=44100.0").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(bodyString()).isNotNull().all {
                        isNotEmpty()
                        elements().each { it.matches(elementRegex) }
                    }
                }
            }
        }

        describe("Built-in types") {
            describe("FftSample") {
                val o = 440.sine().trim(100).window(401).fft(512).toTable("fft")
                val elementRegex = elementRegex(
                    "\\{" +
                            "\"index\":\\d+," +
                            "\"binCount\":\\d+," +
                            "\"samplesCount\":\\d+," +
                            "\"sampleRate\":[\\d\\.]+," +
                            "\"magnitude\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"phase\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"frequency\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"time\":\\d+" +
                            "\\}"
                )

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }
                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    javalin.handleRequest(GET, "/table/fft/last?interval=100ms").apply {
                        assertThat(status).isNotNull().isEqualTo(Status.OK)
                        assertThat(bodyString()).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
            }
            describe("Window<Sample>") {
                val o = 440.sine().trim(100).window(401).toTable("windowSample")
                val elementRegex = elementRegex(
                    "\\{" +
                            "\"size\":\\d+," +
                            "\"step\":\\d+," +
                            "\"elements\":\\[[-eE\\d\\.\\,]+\\]," +
                            "\"sampleType\":\"[\\w\\.$\\_]+\"" +
                            "\\}"
                )

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }
                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    javalin.handleRequest(GET, "/table/windowSample/last?interval=100ms").apply {
                        assertThat(status).isNotNull().isEqualTo(Status.OK)
                        assertThat(bodyString()).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
            }
            describe("List<Double>") {
                val o =
                    440.sine().trim(100).window(401).map { it.elements.map { it.asDouble() } }.toTable("listDouble")
                val elementRegex = elementRegex("\\[[-eE\\d\\.\\,]+\\]")

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }
                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    javalin.handleRequest(GET, "/table/listDouble/last?interval=100ms").apply {
                        assertThat(status).isNotNull().isEqualTo(Status.OK)
                        assertThat(bodyString()).isNotNull().all {
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
                    javalin.handleRequest(GET, "/table/listInt/last?interval=100ms").apply {
                        assertThat(status).isNotNull().isEqualTo(Status.OK)
                        assertThat(bodyString()).isNotNull().all {
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
                val elementRegex = elementRegex(
                    "\\{" +
                            "\"size\":\\d+," +
                            "\"step\":\\d+," +
                            "\"elements\":\\[(\\{\"v\":[-\\d]+},?)+\\]," +
                            "\"sampleType\":\"[\\w\\.$\\_]+\"" +
                            "\\}"
                )

                val overseer = SingleThreadedOverseer(listOf(o))
                beforeGroup {
                    overseer.eval(44100.0f).all { it.get(10000, TimeUnit.MILLISECONDS).finished }
                }
                afterGroup { overseer.close() }

                it("should return last 100ms") {
                    javalin.handleRequest(GET, "/table/windowA/last?interval=100ms").apply {
                        assertThat(status).isNotNull().isEqualTo(Status.OK)
                        assertThat(bodyString()).isNotNull().all {
                            isNotEmpty()
                            elements().each { it.matches(elementRegex) }
                        }
                    }
                }
            }
        }
    }

    describe("AudioService") {

        describe("Streaming Wav") {
            val o = 440.sine().trim(2000).toSampleTable("mySampleTable", 1.s)
            val o2 = 440.sine().trim(2000).toSampleTable("mySampleVectorTable", 1.s, 441)
            val overseer = MultiThreadedOverseer(listOf(o, o2), 2, 1)

            beforeGroup { overseer.eval(44100.0f).all { it.get().finished } }

            afterGroup { overseer.close() }

            it("should return 404 for non-existing table") {
                javalin.handleRequest(GET, "/audio/nonExistingTable/stream/wav").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.NOT_FOUND)
                }
            }
            it("should return stream for existing table in 8 bit format") {
                javalin.handleRequest(GET, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=8").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(headers)
                        .contentType()
                        .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(body.stream.readBytes())
                        .isNotNull().all {
                            size().isEqualTo(44 + 1 * 4410)
                            prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                .isEqualTo(WavHeader(BitDepth.BIT_8, 44100.0f, 1, Int.MAX_VALUE).header())
                        }
                }
            }
            it("should return stream for existing table in 16 bit format") {
                javalin.handleRequest(GET, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=16").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(headers)
                        .contentType()
                        .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(body.stream.readBytes())
                        .isNotNull().all {
                            size().isEqualTo(44 + 2 * 4410)
                            prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
                        }
                }
            }
            it("should return stream for existing table in 24 bit format") {
                javalin.handleRequest(GET, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=24").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(headers)
                        .contentType()
                        .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(body.stream.readBytes())
                        .isNotNull().all {
                            size().isEqualTo(44 + 3 * 4410)
                            prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                .isEqualTo(WavHeader(BitDepth.BIT_24, 44100.0f, 1, Int.MAX_VALUE).header())
                        }
                }
            }
            it("should return stream for existing table in 32 bit format") {
                javalin.handleRequest(GET, "/audio/mySampleTable/stream/wav?limit=100ms&offset=1s&bitDepth=32").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(headers)
                        .contentType()
                        .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(body.stream.readBytes())
                        .isNotNull().all {
                            size().isEqualTo(44 + 4 * 4410)
                            prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                .isEqualTo(WavHeader(BitDepth.BIT_32, 44100.0f, 1, Int.MAX_VALUE).header())
                        }
                }
            }
            it("should return 400 for unknown bit depth") {
                javalin.handleRequest(GET, "/audio/nonExistingTable/stream/wav?limit=100ms&offset=1s&bitDepth=42")
                    .apply {
                        assertThat(status).isNotNull().isEqualTo(Status.BAD_REQUEST)
                    }
            }
            it("should return stream for existing sample vector table") {
                javalin.handleRequest(GET, "/audio/mySampleVectorTable/stream/wav?limit=100ms&offset=1s").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(headers)
                        .contentType()
                        .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(body.stream.readBytes())
                        .isNotNull().all {
                            size().isEqualTo(44 + 2 * 4410)
                            prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
                        }
                }
            }
            it("should return stream limited with 200ms") {
                javalin.handleRequest(GET, "/audio/mySampleTable/stream/wav?limit=200ms&offset=1s").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.OK)
                    assertThat(headers)
                        .contentType()
                        .isEqualTo(AudioStreamOutputFormat.WAV.contentType.toString())
                    assertThat(body.stream.readBytes())
                        .isNotNull().all {
                            size().isEqualTo(44 + 2 * 8820)
                            prop("First 44 bytes") { it.copyOfRange(0, 44) }
                                .isEqualTo(WavHeader(BitDepth.BIT_16, 44100.0f, 1, Int.MAX_VALUE).header())
                        }
                }
            }
            it("should return 400 for unrecognized limit") {
                javalin.handleRequest(GET, "/audio/mySampleTable/stream/wav?limit=100megaseconds").apply {
                    assertThat(status).isNotNull().isEqualTo(Status.BAD_REQUEST)
                }
            }
        }
    }
})

private fun Assert<org.http4k.core.Headers>.contentType() =
    this.prop("Content-Type") { it.find { it.first.toLowerCase() == "content-type" }?.second }

private fun Assert<String>.elements(): Assert<List<String>> =
    this.prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }

private val client by lazy { OkHttp() }

private fun Javalin.handleRequest(method: Method, path: String): Response {
    return client(Request(method, "http://localhost:${this.port()}$path"))
}

