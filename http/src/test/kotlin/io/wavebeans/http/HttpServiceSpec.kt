package io.wavebeans.http

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
import io.wavebeans.lib.asLong
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.s
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.table.toTable
import kotlinx.serialization.Serializable
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
object HttpServiceSpec : Spek({

    describe("TableService") {

        fun elementRegex(valueRegex: String) = Regex("\\{\"offset\":\\d+,\"value\":$valueRegex}")

        val engine = TestApplicationEngine(createTestEnvironment())
        engine.start(wait = false)
        engine.application.tableService()
        with(engine) {
            describe("Samples") {
                val o = 440.sine().toTable("table1", 1.s)
                val elementRegex = elementRegex("-?\\d+\\.\\d+([eE]?-\\d+)?")

                val overseer = LocalOverseer(listOf(o))
                overseer.eval(44100.0f)
                after { overseer.close() }

                it("should return last 100ms") {
                    handleRequest(Get, "/table/table1/last/100ms/44100.0").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }.each { it.matches(elementRegex) }
                        }
                    }
                }
                it("should return time range") {
                    handleRequest(Get, "/table/table1/timeRange/50ms/100ms/").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }.each { it.matches(elementRegex) }
                        }
                    }
                }
            }

            describe("Custom class") {

                @Serializable
                data class A(val v: Long)

                SampleCountMeasurement.registerType(A::class) { 1 }

                val o = 440.sine().map { A(it.asLong()) }.toTable("table2", 1.s)
                val elementRegex = elementRegex("\\{\"v\":-?\\d+}")

                val overseer = LocalOverseer(listOf(o))
                overseer.eval(44100.0f)
                after { overseer.close() }

                it("should return last 100ms") {
                    handleRequest(Get, "/table/table2/last/100ms/44100.0").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }.each { it.matches(elementRegex) }
                        }
                    }
                }
                it("should return time range") {
                    handleRequest(Get, "/table/table2/timeRange/50ms/100ms/44100.0").apply {
                        assertThat(response.status()).isNotNull().isEqualTo(HttpStatusCode.OK)
                        assertThat(response.content).isNotNull().all {
                            isNotEmpty()
                            prop("elements") { it.split("[\\r\\n]".toRegex()).filterNot { it.isEmpty() } }.each { it.matches(elementRegex) }
                        }
                    }

                }
            }
        }
    }
})