package io.wavebeans.lib.io

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.size
import io.wavebeans.lib.at
import io.wavebeans.lib.samplesCountToLength
import io.wavebeans.lib.seqStream
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

object CsvStreamOutputSpec : Spek({
    describe("Sample to csv") {

        val file = File.createTempFile("test_", ".tmp").also { it.deleteOnExit() }
        seqStream()
                .trim(10)
                .toCsv(
                        file.toURI().toString(),
                        header = listOf("time ms", "sample value"),
                        elementSerializer = { idx, sampleRate, sample ->
                            val sampleTime = samplesCountToLength(idx, sampleRate, TimeUnit.MILLISECONDS)
                            listOf(sampleTime.toString(), String.format("%.10f", sample))
                        }
                ).write(200.0f)

        val content = file.readLines()
        it("should not be empty") {
            assertThat(content).all {
                size().isEqualTo(3)
                at(0).isEqualTo("time ms,sample value")
                at(1).isEqualTo("0,0.0000000000")
                at(2).isEqualTo("5,0.0000000001")
            }
        }
    }
    describe("Window<Sample> to csv") {

        val file = File.createTempFile("test_", ".tmp").also { it.deleteOnExit() }
        seqStream()
                .trim(20)
                .window(2)
                .toCsv(
                        file.toURI().toString(),
                        header = listOf("time ms") + (0..1).map { "sample#$it" },
                        elementSerializer = { idx, sampleRate, window ->
                            val sampleTime = samplesCountToLength(idx, sampleRate, TimeUnit.MILLISECONDS)
                            listOf(sampleTime.toString()) + window.elements.map { String.format("%.10f", it) }
                        }
                ).write(200.0f)

        val content = file.readLines()
        it("should not be empty") {
            assertThat(content).all {
                size().isEqualTo(3)
                at(0).isEqualTo("time ms,sample#0,sample#1")
                at(1).isEqualTo("0,0.0000000000,0.0000000001")
                at(2).isEqualTo("5,0.0000000002,0.0000000003")
            }
        }
    }
    describe("Custom object to csv") {

        val file = File.createTempFile("test_", ".tmp").also { it.deleteOnExit() }
        seqStream()
                .trim(10)
                .map { Pair(it, it * 2) }
                .toCsv(
                        file.toURI().toString(),
                        header = listOf("time ms") + (0..1).map { "value#$it" },
                        elementSerializer = { idx, sampleRate, pair ->
                            val sampleTime = samplesCountToLength(idx, sampleRate, TimeUnit.MILLISECONDS)
                            listOf(sampleTime.toString(), String.format("%.10f", pair.first), String.format("%.10f", pair.second))
                        }
                ).write(200.0f)

        val content = file.readLines()
        it("should not be empty") {
            assertThat(content).all {
                size().isEqualTo(3)
                at(0).isEqualTo("time ms,value#0,value#1")
                at(1).isEqualTo("0,0.0000000000,0.0000000000")
                at(2).isEqualTo("5,0.0000000001,0.0000000002")
            }
        }
    }
})

private fun <T : Any> StreamOutput<T>.write(sampleRate: Float) {
    this.writer(sampleRate).use { w ->
        while (w.write()) {
            sleep(0)
        }
    }
}