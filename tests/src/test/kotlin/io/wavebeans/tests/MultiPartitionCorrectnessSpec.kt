package io.wavebeans.tests

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import io.wavebeans.execution.MultiThreadedOverseer
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.fft.inverseFft
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.hamming
import io.wavebeans.lib.stream.window.window
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.toTable
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureTimeMillis

private val log = KotlinLogging.logger {}

object MultiPartitionCorrectnessSpec : Spek({

    fun runInParallel(
            outputs: List<StreamOutput<out Any>>,
            threads: Int = 2,
            partitions: Int = 2,
            sampleRate: Float = 44100.0f
    ): Long {

        val runTime = measureTimeMillis {
            MultiThreadedOverseer(outputs, threads, partitions).use { overseer ->
                assertThat(
                        overseer.eval(sampleRate)
                                .map { it.get() }
                                .also {
                                    it
                                            .mapNotNull { it.exception }
                                            .map { log.error(it) { "Error during evaluation" }; it }
                                            .firstOrNull()
                                            ?.let { throw it }
                                }
                                .all { it.finished }
                ).isTrue()
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
            SingleThreadedOverseer(outputs).use { overseer ->
                assertThat(
                        overseer.eval(sampleRate)
                                .map { it.get() }
                                .also {
                                    it
                                            .mapNotNull { it.exception }
                                            .map { log.error(it) { "Error during evaluation" }; it }
                                            .firstOrNull()
                                            ?.let { throw it }
                                }
                                .all { it.finished }
                ).isTrue()
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
                .hamming()
                .fft(512)
        val o3 = fft.magnitudeToCsv("file://${f3.absolutePath}")
        val o4 = fft.phaseToCsv("file://${f4.absolutePath}")

        val outputs = listOf(o1, o2, o3, o4)

        val runTime = runInParallel(outputs)

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

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

            runInParallel(o)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

            runLocally(o)
            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
    }

    describe("Table output") {
        val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

        val run1 = seqStream().trim(1000).toTable("t1")
        val run2 = TableRegistry.default.byName<Sample>("t1")
                .last(2000.ms)
                .map { it * 2 }
                .toCsv("file://${file.absolutePath}")

        runInParallel(listOf(run1))

        runInParallel(listOf(run2))

        val fileContent = file.readLines()

        it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

        TableRegistry.default.reset("t1")

        runLocally(listOf(run1))
        runLocally(listOf(run2))

        val fileContentLocal = file.readLines()
        it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }

    }

    describe("Exception throwing by overseer") {
        data class SomeUnknownClass(val value: Int)

        val run1 = seqStream().map { SomeUnknownClass(1) }.trim(100).toDevNull()

        it("should throw exception when run in parallel") {
            assertThat(catch { runInParallel(listOf(run1)) }).isNotNull()
        }
        it("should  throw exception when run in single thread") {
            assertThat(catch { runLocally(listOf(run1)) }).isNotNull()
        }
    }

    describe("Table output with FftSample") {
        val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

        val run1 = seqStream()
                .window(401)
                .fft(512)
                .trim(10)
                .toTable("t2", 10.s)

        val run2 = TableRegistry.default.byName<FftSample>("t2")
                .last(2000.ms)
                .map { it.magnitude().toList() }
                .toCsv(
                        uri = "file://${file.absolutePath}",
                        header = listOf("index", "magnitudes"),
                        elementSerializer = { (idx, _, magnitudes) ->
                            listOf(
                                    idx.toString(),
                                    magnitudes.joinToString(",")
                            )
                        }
                )

        runInParallel(listOf(run1))

        runInParallel(listOf(run2))

        val fileContent = file.readLines()

        it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

        TableRegistry.default.reset("t2")

        runLocally(listOf(run1))
        runLocally(listOf(run2))

        val fileContentLocal = file.readLines()
        it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }

    }

    describe("Concatenation") {
        describe("finite..finite") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

            val stream1 = seqStream().trim(1000)
            val stream2 = seqStream().trim(1000)
            val concatenation = (stream1..stream2).toCsv("file://${file.absolutePath}")

            runInParallel(listOf(concatenation), partitions = 2)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

            runLocally(listOf(concatenation))

            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
        describe("finite..infinite") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

            val stream1 = seqStream().trim(1000)
            val stream2 = seqStream()
            val concatenation = (stream1..stream2)
                    .trim(5000)
                    .toCsv("file://${file.absolutePath}")

            runInParallel(listOf(concatenation), partitions = 2)

            val fileContent = file.readLines()

            it("should have non-empty output") { assertThat(fileContent).size().isGreaterThan(1) }

            runLocally(listOf(concatenation))

            val fileContentLocal = file.readLines()
            it("should have the same output as local") { assertThat(fileContent).isEqualTo(fileContentLocal) }
        }
    }

    describe("Flatten") {
        it("should have the same output as local") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val stream = seqStream()
                    .trim(1000)
                    .window(128)
                    .map {
                        it.elements
                                .zip(it.elements)
                                .flatMap { listOf(it.first, it.second) }
                    }
                    .flatten()
                    .toCsv("file://${file.absolutePath}")

            runInParallel(listOf(stream), partitions = 2)
            val fileContent = file.readLines()
            assertThat(fileContent).size().isGreaterThan(1)

            runLocally(listOf(stream))
            val fileContentLocal = file.readLines()
            assertThat(fileContent).isEqualTo(fileContentLocal)
        }
    }

    describe("Resample") {
        it("should have the same output as local") {
            val sampleRate = 44100.0f
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val wavFileStream by lazy {
                val f = File.createTempFile("test", ".wav").also { it.deleteOnExit() }
                1240.sine().trim(10000).toMono16bitWav("file://${f.absolutePath}").evaluate(sampleRate / 2.0f)
                wave("file://${f.absolutePath}")
            }
            val input = (
                    120.sine().trim(100)..440.sine() +
                            (100..110).sineSweep(0.5, 20.0) +
                            input { sampleOf((it.first % 10000L) * 10000L) } +
                            listOf(0.5, 0.3, 0.5, 0.2).input() +
                            wavFileStream
                    ) * 0.2
            val stream = input
                    .trim(1000)
                    .resample(to = sampleRate * 2.0f)
                    .window(1001)
                    .hamming()
                    .fft(1024)
                    .inverseFft()
                    .flatten()
                    .resample()
                    .toCsv("file://${file.absolutePath}")

            runInParallel(listOf(stream), partitions = 2, sampleRate = sampleRate)
            val fileContent = file.readLines()
            assertThat(fileContent).size().isGreaterThan(1)

            runLocally(listOf(stream), sampleRate = sampleRate)
            val fileContentLocal = file.readLines()
            assertThat(fileContent).isEqualTo(fileContentLocal)
        }
    }

    describe("Output as a function") {
        class NewLineDelimiterFile(
                file: String,
                duration: Double,
        ) : Fn<WriteFunctionArgument<Sample>, Boolean>(
                FnInitParameters().add("file", file).add("duration", duration)
        ) {

            private val duration by lazy { initParams.double("duration") }
            private var file: OutputStream? = null

            override fun apply(argument: WriteFunctionArgument<Sample>): Boolean {
                if (file == null) {
                    file = File(initParams.string("file")).outputStream().buffered()
                }
                if (argument.phase == WriteFunctionPhase.WRITE) {
                    file!!.write(String.format("%.10f\n", argument.sample!!.asDouble()).toByteArray())
                } else if (argument.phase == WriteFunctionPhase.CLOSE) {
                    file!!.close()
                    file = null
                }
                return argument.sampleIndex / argument.sampleRate < duration
            }
        }
        it("should have the same output as local") {
            val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val stream = seqStream()
                    .out(NewLineDelimiterFile(file.absolutePath, 0.1))

            runInParallel(listOf(stream))
            val fileContent = file.readLines()
            assertThat(fileContent).size().isEqualTo(4411)

            runLocally(listOf(stream))
            val fileContentLocal = file.readLines()
            assertThat(fileContent).isEqualTo(fileContentLocal)
        }
    }
})
