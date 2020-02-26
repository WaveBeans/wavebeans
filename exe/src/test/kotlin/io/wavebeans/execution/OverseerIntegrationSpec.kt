package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.window
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureTimeMillis

val log = KotlinLogging.logger {}

@ExperimentalStdlibApi
object OverseerIntegrationSpec : Spek({

    @ExperimentalStdlibApi
    fun runOnOverseer(
            outputs: List<StreamOutput<out Any>>,
            threads: Int = 2,
            partitions: Int = 2,
            sampleRate: Float = 44100.0f
    ): Long {

        val runTime = measureTimeMillis {
            LocalDistributedOverseer(outputs, threads, partitions).use { overseer ->
                assertThat(overseer.eval(sampleRate).all { it.get() == true }).isTrue()
            }
        }
        log.debug { "Distributed run finished in $runTime ms" }
        return runTime
    }

    fun runLocally(
            outputs: List<StreamOutput<out Any>>,
            sampleRate: Float = 44100.0f
    ): Long {
        val runTime = measureTimeMillis {
            LocalOverseer(outputs).use { overseer ->
                assertThat(overseer.eval(sampleRate).all { it.get() == true }).isTrue()
            }
        }
        log.debug { "Local run finished in $runTime ms" }
        return runTime
    }

    describe("Two outputs with different paths but same content") {
        val f1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f3 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f4 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

        val i1 = seqStream()
        val i2 = seqStream()

        val length = 50L

        val p1 = i1.changeAmplitude(1.0).rangeProjection(-100, length)
        val p2 = i2.changeAmplitude(0.0)

        val o1 = p1
                .trim(length)
                .toCsv("file://${f1.absolutePath}")
        val pp = p1 + p2
        val o2 = pp
                .trim(length)
                .toCsv("file://${f2.absolutePath}")
        val fft = pp
                .trim(length)
                .window(401)
                .fft(512)
        val o3 = fft.magnitudeToCsv("file://${f3.absolutePath}")
        val o4 = fft.phaseToCsv("file://${f4.absolutePath}")

        val outputs = listOf(o1, o2, o3, o4)

        val runTime = runOnOverseer(outputs)

        val f1Content = f1.readLines()
        val f2Content = f2.readLines()
        val f3Content = f3.readLines()
        val f4Content = f4.readLines()
        it("should have non empty output. 1st file") { assertThat(f1Content).size().isGreaterThan(1) }
        it("should have non empty output. 2nd file") { assertThat(f2Content).size().isGreaterThan(1) }
        it("should have the same output") { assertThat(f1Content).isEqualTo(f2Content) }

        val localRunTime = runLocally(outputs)

        val f1LocalContent = f1.readLines()
        it("should have the same size as local content [1]") { assertThat(f1Content.size).isEqualTo(f1LocalContent.size) }
        it("should have the same output as local content [1]") { assertThat(f1Content).isEqualTo(f1LocalContent) }
        val f2LocalContent = f2.readLines()
        it("should have the same size as local content [2]") { assertThat(f2Content.size).isEqualTo(f2LocalContent.size) }
        it("should have the same output as local content [2]") { assertThat(f2Content).isEqualTo(f2LocalContent) }
        val f3LocalContent = f3.readLines()
        it("should have the same size as local content [3]") { assertThat(f3Content.size).isEqualTo(f3LocalContent.size) }
        it("should have the same output as local content [3]") { assertThat(f3Content).isEqualTo(f3LocalContent) }
        val f4LocalContent = f4.readLines()
        it("should have the same size as local content [4]") { assertThat(f4Content.size).isEqualTo(f4LocalContent.size) }
        it("should have the same output as local content [4]") { assertThat(f4Content).isEqualTo(f4LocalContent) }

        log.debug { "Took $runTime ms, local run time is $localRunTime ms" }
    }

    describe("Map function") {

        describe("Sample to Sample mapping") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val o = listOf(
                    seqStream()
                            .map { it * 2 }
                            .trim(100)
                            .toCsv("file://${file.absolutePath}")
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should not return empty input ") { assertThat(fileContent).isNotEmpty() }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }

        }

        describe("Window<Sample> to Sample mapping") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val o = listOf(
                    seqStream()
                            .window(10)
                            .map { w -> w.elements.last() }
                            .trim(100)
                            .toCsv("file://${file.absolutePath}")
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).isNotEmpty() }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }

        }

        describe("Sample to Window<Sample> and back mapping") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val o = listOf(
                    seqStream()
                            .map { sample -> Window.ofSamples(10, 10, (0..9).map { sample }) }
                            .map { window -> window.elements.first() }
                            .trim(100)
                            .toCsv("file://${file.absolutePath}")
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).isNotEmpty() }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }

        }
    }

    describe("Function Input") {
        describe("generating sinusoid") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val o = listOf(
                    input { (x, sampleRate) -> sampleOf(1.0 * cos(x / sampleRate * 2.0 * PI * 440.0)) }
                            .trim(100)
                            .toCsv("file://${file.absolutePath}")
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).isNotEmpty() }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
    }

    describe("Function Merge") {
        describe("generating sinusoid and merging it with another function") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val timeTickInput = input { (x, sampleRate) -> sampleOf(x.toDouble() / sampleRate) }
            val o = listOf(
                    220.sine()
                            .merge(with = timeTickInput) { (x, y) ->
                                x + sampleOf(1.0 * sin((y ?: ZeroSample) * 2.0 * PI * 440.0))
                            }
                            .trim(100)
                            .toCsv("file://${file.absolutePath}")
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).isNotEmpty() }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
    }

    describe("CSV output with function") {
        describe("generating sinusoid and storing it to csv") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val o = listOf(
                    220.sine()
                            .trim(100)
                            .toCsv(
                                    "file://${file.absolutePath}",
                                    header = listOf("sample index", "sample value"),
                                    elementSerializer = { (idx, _, sample) ->
                                        listOf(idx.toString(), String.format("%.10f", sample))
                                    }
                            )
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).isNotEmpty() }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
    }

    describe("List as Input") {
        describe("generating list of samples and storing it to csv") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val o = listOf(
                    listOf(1, 2, 3, 4).map { sampleOf(it) }.input()
                            .toCsv(
                                    "file://${file.absolutePath}",
                                    header = listOf("sample index", "sample value"),
                                    elementSerializer = { (idx, _, sample) ->
                                        listOf(idx.toString(), String.format("%.10f", sample))
                                    }
                            )
            )

            runOnOverseer(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
    }
})
