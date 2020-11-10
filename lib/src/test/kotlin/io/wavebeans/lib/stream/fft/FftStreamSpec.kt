package io.wavebeans.lib.stream.fft

import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.tests.eachIndexed
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.sineSweep
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.rangeTo
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.math.PI

class FftStreamSpec : Spek({
    describe("Given sinusoid 32Hz, sample rate 128Hz, 2seconds, amplitude=0.5") {
        val sine = 32.sine(0.5)

        describe("Calculating FFT") {
            val fft = sine.trim(2000)
                    .window(256)
                    .fft(256)
                    .asSequence(128.0f)
                    .take(1)
                    .toList()


            it("fft stream length should be 1") { assertThat(fft.size).isEqualTo(1) }


            describe("First sample of fft stream") {
                val fft1 = fft[0]

                it("should have magnitude with spike at 1st bin") {
                    assertThat(fft1.magnitude().toList()).eachIndexed { it, idx ->
                        when (idx) {
                            64 -> it.isCloseTo(36.12359947967774, 1e-12)
                            else -> it.isLessThan(-250.0)
                        }
                    }
                }

                it("should have phase always less than 2*PI") {
                    assertThat(fft1.phase().toList()).each {
                        it.isLessThan(2 * PI)
                        it.isGreaterThan(-2 * PI)
                    }
                }
            }
        }
    }

    describe("Given sinusoid 440Hz, sample rate 44100Hz, 0.5 seconds, amplitude=0.5") {
        val sine = 440.sine(0.5)

        describe("Calculating FFT") {
            val fft = sine.trim(500)
                    .window(1024)
                    .fft(1024)
                    .asSequence(44100.0f)

            describe("The sample of fft stream") {
                val fft1 = fft.first()

                it("should have magnitude with spike at 10th bin") {
                    assertThat(fft1.magnitude().toList()).eachIndexed { it, idx ->
                        when (idx) {
                            9 -> it.isCloseTo(32.0, 1.0)
                            10 -> it.isCloseTo(47.50808284344064, 1e-12)
                            11 -> it.isCloseTo(36.0, 1.0)
                            else -> it.isLessThan(30.0)
                        }
                    }
                }

                it("should have phase always less than 2*PI") {
                    assertThat(fft1.phase().toList()).each {
                        it.isLessThan(2 * PI)
                        it.isGreaterThan(-2 * PI)
                    }
                }
            }
        }
    }

    describe("Inverse FFT") {
        val signals = listOf(
                "440Hz sine" to 440.sine(),
                "440Hz+1230Hz sine" to (440.sine() + 1230.sine()),
                "440Hz..1230Hz sine sweep" to ((440..1230).sineSweep(0.5, 0.1, sweepDelta = 0.01)),
                "312Hz,440Hz,1230Hz concatenated sines" to (312.sine().trim(30)..440.sine().trim(30)..1230.sine()),
        )

        signals.forEach { (name, signal) ->
            it("should return the same signal after inverse transformation: $name") {
                val n = 4096
                val l = signal
                        .window(501)
                        .fft(512)
                        .inverseFft()
                        .asSequence(44100.0f)
                        .flatMap { it.elements }
                        .take(n)
                        .toList()

                val e = signal.asSequence(44100.0f).take(n).toList().toTypedArray()
                assertThat(l).eachIndexed(n) { sample, index ->
                    sample.isCloseTo(e[index], 1e-10)
                }
            }

        }
    }
})