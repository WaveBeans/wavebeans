package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import assertk.fail
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.*
import io.wavebeans.lib.isListOf
import io.wavebeans.lib.sampleOf
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.fft.inverseFft
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.evaluate
import io.wavebeans.tests.isContainedBy
import io.wavebeans.tests.toList
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.*
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.math.abs

object ResampleStreamSpec : Spek({
    describe("Resampling the input to match the output") {

        it("should upsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }.resample()

            assertThat(resampled.toList(2000.0f)).isListOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4)
        }

        it("should downsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }.resample(resampleFn = SimpleResampleFn { it.sum() })

            assertThat(resampled.toList(500.0f)).isListOf(1, 5, 4)
        }
        it("should resample with custom function") {
            fun resample(a: ResamplingArgument<Int>): Sequence<Int> {
                require(a.inputOutputFactor == 0.5f)
                return a.inputSequence.map { listOf(it, -1) }.flatten()
            }

            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }.resample(resampleFn = ::resample)

            assertThat(resampled.toList(500.0f)).isListOf(0, -1, 1, -1, 2, -1, 3, -1, 4, -1)
        }
    }

    describe("Resampling the input to reprocess and then to match the output") {
        it("should upsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }
                    .resample(to = 2000.0f)
                    .map { it * 2 }
                    .resample()

            assertThat(resampled.toList(4000.0f)).isListOf(0, 0, 0, 0, 2, 2, 2, 2, 4, 4, 4, 4, 6, 6, 6, 6, 8, 8, 8, 8)
        }

        it("should downsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }
                    .resample(to = 500.0f, resampleFn = SimpleResampleFn { it.sum() })
                    .map { it * 2 }
                    .resample(resampleFn = SimpleResampleFn { it.sum() })

            assertThat(resampled.toList(250.0f)).isListOf(12, 8)
        }

        it("should resample with custom function") {
            fun resample(a: ResamplingArgument<Int>): Sequence<Int> {
                require(a.inputOutputFactor == 2.5f || a.inputOutputFactor == 0.1f)
                return if (a.inputOutputFactor == 2.5f)
                    a.inputSequence.map { listOf(it, -1) }.flatten()
                else
                    a.inputSequence.map { listOf(it, -3) }.flatten()
            }

            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }
                    .resample(to = 2500.0f, resampleFn = ::resample)
                    .map { it * 2 }
                    .resample(resampleFn = ::resample)

            assertThat(resampled.toList(250.0f)).isListOf(
                    0, -3, -2, -3,
                    2, -3, -2, -3,
                    4, -3, -2, -3,
                    6, -3, -2, -3,
                    8, -3, -2, -3,
            )
        }
    }

    describe("Samples in wav-files") {
        val input = (440.sine() * 0.2).trim(1000)
        val outputFile by memoized(SCOPE) { File.createTempFile("temp", ".wav") }
        val streamFromProcessedWavfFile by memoized(SCOPE) {
            input
                    .resample(to = 16000.0f)
                    .resample()
                    .toMono16bitWav("file://${outputFile.absolutePath}")
                    .evaluate(8000.0f)
            wave("file://${outputFile.absolutePath}")
        }

        it("should resample 8000Hz sample rate to 4000Hz after reading from file") {
            val samples = streamFromProcessedWavfFile.resample().toList(4000.0f).take(1000)
            assertThat(samples).isContainedBy(input.toList(4000.0f, take = 1000)) { a, b -> abs(a - b) < 0.1 }
        }
        it("should resample 8000Hz sample rate to 4000Hz after reading from file and mixing it with the generator") {
            val generatorStream = (880.sine() * 0.2).trim(1000)
            val samples = (streamFromProcessedWavfFile + generatorStream).resample().toList(4000.0f).take(1000)
            assertThat(samples).isContainedBy((input + generatorStream).toList(4000.0f, take = 1100)) { a, b -> abs(a - b) < 0.1 }
        }
        it("should resample 8000Hz sample rate to 16000Hz after reading from file") {
            val samples = streamFromProcessedWavfFile.resample().toList(16000.0f).take(1000)
            assertThat(samples).isContainedBy(input.toList(16000.0f, take = 1000)) { a, b -> abs(a - b) < 0.1 }
        }
        it("should resample 8000Hz sample rate to 16000Hz after reading from file and mixing it with the generator") {
            val generatorStream = (880.sine() * 0.2).trim(1000)
            val samples = (streamFromProcessedWavfFile + generatorStream).resample().toList(16000.0f).take(1000)
            assertThat(samples).isContainedBy((input + generatorStream).toList(16000.0f, take = 1000)) { a, b -> abs(a - b) < 0.1 }
        }
        it("should resample 8000Hz sample rate to 16000Hz after reading from file and passing through FFT-Inverse FFT process") {
            val samples = streamFromProcessedWavfFile
                    .window(1001)
                    .fft(1024)
                    .inverseFft()
                    .flatten()
                    .resample()
                    .toList(16000.0f)
                    .take(2000)
            assertThat(samples).isContainedBy(input.toList(16000.0f, take = 2000)) { a, b -> abs(a - b) < 0.1 }
        }
        it("should fail streaming without resample() via /dev/null writer") {
            val e = catch { streamFromProcessedWavfFile.toDevNull().evaluate(16000.0f) }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail streaming without resample() via wav-writer") {
            val e = catch { streamFromProcessedWavfFile.toMono16bitWav("file:///anyfile.wav").evaluate(16000.0f) }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail streaming without resample() via partial wav-writer") {
            val e = catch {
                streamFromProcessedWavfFile
                        .map { it.withOutputSignal<Sample, Unit>(NoopOutputSignal) }
                        .toMono16bitWav("file:///anyfile.csv", suffix = { fail("Unreachable statement") })
                        .evaluate(16000.0f)
            }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail streaming without resample() via csv-writer") {
            val e = catch { streamFromProcessedWavfFile.toCsv("file:///anyfile.csv").evaluate(16000.0f) }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail streaming without resample() via FFT csv-writer") {
            val e = catch { streamFromProcessedWavfFile.window(20).fft(32).magnitudeToCsv("file:///anyfile.csv").evaluate(16000.0f) }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail streaming without resample() via partial csv-writer") {
            val e = catch {
                streamFromProcessedWavfFile
                        .map { it.withOutputSignal<Sample, Unit>(NoopOutputSignal) }
                        .toCsv("file:///anyfile.csv", suffix = { fail("Unreachable statement") })
                        .evaluate(16000.0f)
            }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail streaming without resample() via function writer") {
            val e = catch { streamFromProcessedWavfFile.out { fail("unreachable statement") }.evaluate(16000.0f) }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz before writing")
        }
        it("should fail when streaming without resample() via asSequence") {
            val e = catch { streamFromProcessedWavfFile.asSequence(16000.0f).toList() }
            assertThat(e).isNotNull()
                    .message().isNotNull().endsWith("The stream should be resampled from 8000.0Hz to 16000.0Hz")
        }
    }
})
