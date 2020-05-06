package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.Sample
import io.wavebeans.lib.math.i
import io.wavebeans.lib.sampleOf
import io.wavebeans.lib.stream.SampleCountMeasurement.samplesInObject
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SampleCountMeasurementSpec : Spek({
    describe("Measuring builtin types") {
        it("should measure samples") {
            val obj = sampleOf(1)
            assertThat(samplesInObject(obj)).isEqualTo(1)
        }
        it("should measure list of samples") {
            val obj = listOf(sampleOf(1), sampleOf(1))
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure window of samples") {
            val obj = Window.ofSamples(3, 2, listOf(sampleOf(1), sampleOf(1), sampleOf(1), sampleOf(1)))
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure numbers: ints") {
            val obj = 1
            assertThat(samplesInObject(obj)).isEqualTo(1)
        }
        it("should measure list of numbers: ints") {
            val obj = listOf(1, 2, 3, 4)
            assertThat(samplesInObject(obj)).isEqualTo(4)
        }
        it("should measure window of ints") {
            val obj = Window(3, 2, listOf(1, 2, 3, 4)) { 0 }
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure numbers: doubles") {
            val obj = 1.0
            assertThat(samplesInObject(obj)).isEqualTo(1)
        }
        it("should measure list of numbers: doubles") {
            val obj = listOf(1.0, 2.0)
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure window of doubles") {
            val obj = Window(3, 2, listOf(1.0, 2.0, 3.0, 4.0)) { 0.0 }
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure numbers: floats") {
            val obj = 1.0f
            assertThat(samplesInObject(obj)).isEqualTo(1)
        }
        it("should measure list of numbers: floats") {
            val obj = listOf(1.0f, 2.0f, 3.0f)
            assertThat(samplesInObject(obj)).isEqualTo(3)
        }
        it("should measure window of floats") {
            val obj = Window(3, 2, listOf(1.0f, 2.0f, 3.0f, 4.0f)) { 0.0f }
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure FFT samples") {
            val obj = FftSample(0, 4, 4, 2, 1.0f, listOf(0.i, 0.i, 0.i, 0.i))
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
        it("should measure list of FFT samples") {
            val obj = listOf(
                    FftSample(0, 4, 4, 2, 1.0f, listOf(0.i, 0.i, 0.i, 0.i)),
                    FftSample(0, 4, 4, 4, 1.0f, listOf(0.i, 0.i, 0.i, 0.i))
            )
            assertThat(samplesInObject(obj)).isEqualTo(6)
        }
    }

    describe("Measuring custom types via interface") {

        data class MySample(val v: Sample) : Measured {
            override fun measure(): Int = samplesInObject(this.v)
        }

        it("should measure samples") {
            val obj = MySample(sampleOf(1))
            assertThat(samplesInObject(obj)).isEqualTo(1)
        }
        it("should measure list of samples") {
            val obj = listOf(MySample(sampleOf(1)), MySample(sampleOf(1)))
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
    }

    describe("Measuring custom types via registerType") {

        data class MySample(val v: Sample)

        SampleCountMeasurement.registerType(MySample::class) { samplesInObject(it.v) }

        it("should measure samples") {
            val obj = MySample(sampleOf(1))
            assertThat(samplesInObject(obj)).isEqualTo(1)
        }
        it("should measure list of samples") {
            val obj = listOf(MySample(sampleOf(1)), MySample(sampleOf(1)))
            assertThat(samplesInObject(obj)).isEqualTo(2)
        }
    }
})