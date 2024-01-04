package io.wavebeans.http

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.io.input
import io.wavebeans.lib.sampleOf
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.lib.stream.trim
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

object JsonBeanStreamReaderSpec : Spek({

    fun elementRegex(valueRegex: String) = Regex("\\{\"offset\":\\d+,\"value\":$valueRegex}")

    describe("Sequence of samples") {
        val seq = input { (i, _) -> sampleOf(i) }.trim(50, TimeUnit.SECONDS)

        it("should have 50 doubles") {
            val lines = JsonBeanStreamReader(seq, 1.0f).bufferedReader().use { it.readLines() }
            assertThat(lines).all {
                size().isEqualTo(50)
                each { it.matches(elementRegex("\\d+\\.\\d+([eE]?-\\d+)?")) }
            }
        }
    }

    describe("Sequence of serializable classes") {
        @Serializable
        data class S(val v: Long)

        SampleCountMeasurement.registerType(S::class) { 1 }

        val seq = input { (i, _) -> S(i) }.trim(50, TimeUnit.SECONDS)

        it("should have 50 objects as json") {
            val lines = JsonBeanStreamReader(seq, 1.0f).bufferedReader().use { it.readLines() }
            assertThat(lines).all {
                size().isEqualTo(50)
                each { it.matches(elementRegex("\\{\"v\":\\d+}")) }
            }
        }
    }

    describe("Sequence of non-serializable classes") {

        data class N(val v: Long)

        SampleCountMeasurement.registerType(N::class) { 1 }

        val seq = input { (i, _) -> N(i) }.trim(50, TimeUnit.SECONDS)

        it("should throw an exception") {
            assertThat {
                JsonBeanStreamReader(seq, 1.0f).bufferedReader()
                    .use<BufferedReader, List<String>> { it.readLines() }
            }
                    .isFailure()
                    .all {
                        message().isNotNull().startsWith("Serializer for class 'N' is not found.\n" +
                                "Mark the class as @Serializable or provide the serializer explicitly.")
                        hasClass(SerializationException::class)
                    }
        }
    }
})