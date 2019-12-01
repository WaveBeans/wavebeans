package io.wavebeans.lib.stream.fft

import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.math.PI

class FftStreamSpec : Spek({
    describe("Given sinusoid 32Hz, sample rate 128Hz, 2seconds, amplitude=0.5") {
        val sine = 32.sine(0.5)

        describe("Calculating FFT") {
            val fft = sine.window(256)
                    .fft(256)
                    .trim(2000)
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
            val fft = sine.window(1024)
                    .fft(1024)
                    .trim(500)
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

})