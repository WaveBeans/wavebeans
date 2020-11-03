package io.wavebeans.lib.io

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.size
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.merge
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.util.concurrent.TimeUnit

object CsvStreamOutputSpec : Spek({
    describe("Sample to csv") {

        val file = File.createTempFile("test_", ".tmp").also { it.deleteOnExit() }
        seqStream()
                .trim(10)
                .toCsv(
                        file.toURI().toString(),
                        header = listOf("time ms", "sample value"),
                        elementSerializer = { (idx, sampleRate, sample) ->
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
                        elementSerializer = { (idx, sampleRate, window) ->
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
                        elementSerializer = { (idx, sampleRate, pair) ->
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

    describe("Partial output") {
        data class IndexedSample(
                val sample: Sample,
                val index: Long
        )

        val outputDir by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = outputDir.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()
        fun BeanStream<Managed<OutputSignal, Long, Sample>>.toCsv(): StreamOutput<Managed<OutputSignal, Long, Sample>> = this.toCsv(
                uri = "file://${outputDir.absolutePath}/test.csv",
                header = listOf("number", "value"),
                elementSerializer = { (i, _, sample) ->
                    listOf("$i", String.format("%.10f", sample))
                },
                suffix = { "-${it ?: 0}" }
        )

        describe("Flush") {
            it("should write sample stream chunked into 10 different files") {
                seqStream()
                        .merge(input { it.first }) { (s, i) -> requireNotNull(s); requireNotNull(i); IndexedSample(s, i) }
                        .map {
                            if (it.index > 0 && it.index % 100 == 0L) {
                                it.sample.withOutputSignal(FlushOutputSignal, it.index / 100)
                            } else {
                                it.sample.withOutputSignal(NoopOutputSignal)
                            }
                        }
                        .trim(1000)
                        .toCsv()
                        .write(1000.0f)

                assertThat(outputFiles()).eachIndexed(10) { file, index ->
                    file.prop("name") { it.nameWithoutExtension }.isEqualTo("test-$index")
                    val offset = index * 100
                    file.prop("content") { it.readText() }.isEqualTo(
                            "number,value\n" +
                                    (offset..(offset + 100)).asSequence()
                                            .zip(seqStream().asSequence(1000.0f).drop(offset).take(100))
                                            .joinToString("\n") { "${it.first},${String.format("%.10f", it.second)}" } +
                                    "\n"

                    )
                }

            }
        }
        describe("Open-close gate") {
            it("should write only even chunks of sample stream into 5 different files") {
                seqStream()
                        .merge(input { it.first }) { (s, i) -> requireNotNull(s); requireNotNull(i); IndexedSample(s, i) }
                        .map {
                            if (it.index > 0 && it.index % 100 == 0L) {
                                val chunkIdx = it.index / 100
                                if (chunkIdx % 2 == 0L)
                                    it.sample.withOutputSignal(OpenGateOutputSignal, chunkIdx)
                                else
                                    it.sample.withOutputSignal(CloseGateOutputSignal, chunkIdx)
                            } else {
                                it.sample.withOutputSignal(NoopOutputSignal)
                            }
                        }
                        .trim(1000)
                        .toCsv()
                        .write(1000.0f)

                assertThat(outputFiles()).eachIndexed(5) { file, index ->
                    val j = index * 2
                    file.prop("name") { it.nameWithoutExtension }.isEqualTo("test-$j")
                    val offset = j * 100
                    file.prop("content") { it.readText() }.isEqualTo(
                            "number,value\n" +
                                    (offset..(offset + 100)).asSequence()
                                            .zip(seqStream().asSequence(1000.0f).drop(offset).take(100))
                                            .joinToString("\n") { "${it.first},${String.format("%.10f", it.second)}" } +
                                    "\n"

                    )
                }

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