package mux.lib

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.catch
import mux.lib.io.SineGeneratedInput
import mux.lib.math.*
import mux.lib.stream.AudioSampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DftSpec : Spek({
    describe("Given signal as array of doubles [1,4]") {
        val x = (1..4).map { it.r }.asSequence()
        val expected = arrayOf(
                10 + 0.i,
                -2 + 2.i,
                -2 + 0.i,
                -2 - 2.i
        )

        describe("Calculating DFT") {
            val dft = dft(x, 4)

            it("should be as specific array") {
                assertThat(dft.toList()).eachIndexed(4) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }

        describe("Calculating FFT") {
            val fft = fft(x, 4)

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(4) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }
    }

    describe("Given DFT=[1,1,1,1]") {
        val x = listOf(1.0, 1.0, 1.0, 1.0).map { it.r }.asSequence()

        describe("Calculating IDFT") {
            val idft = idft(x, 4)
            it("should be as specific array") {
                val expected = arrayOf(
                        1 + 0.i,
                        0 + 0.i,
                        0 + 0.i,
                        0 - 0.i
                )
                assertThat(idft.toList()).eachIndexed(4) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }
    }

    describe("Given sinusoid 64Hz, sample rate 128Hz, 2seconds") {
        val sine = SineGeneratedInput(
                128.0f,
                64.0,
                1.0,
                2.0
        ).asSequence(128.0f).map { it.r }

        describe("Calculating FFT") {
            val fft = fft(sine, 256)

            val expected = Array(256) { if (it == 128) 256.r else 0.r }

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(256) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }
    }

    describe("Given sinusoid 64Hz, sample rate 128Hz, 2seconds, amplitude=0.5") {
        val sine = SineGeneratedInput(
                128.0f,
                64.0,
                0.5,
                2.0
        ).asSequence(128.0f).map { it.r }

        describe("Calculating FFT") {
            val fft = fft(sine, 256)

            val expected = Array(256) { if (it == 128) 128.r else 0.r }

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(256) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }
    }

    describe("Given sinusoids 32 and 64Hz, sample rate 128Hz, 2seconds") {
        val sine1 = AudioSampleStream(SineGeneratedInput(
                128.0f,
                32.0,
                1.0,
                2.0))
        val sine2 = AudioSampleStream(SineGeneratedInput(
                128.0f,
                64.0,
                1.0,
                2.0
        ))
        val x = sine1.mixStream(0, sine2)
                .asSequence(128.0f).map { it.r }

        describe("Calculating FFT") {
            val fft = fft(x, 256)

            val expected = Array(256) {
                if (it == 128) 256.r
                else if (it == 64 || it == 192) 128.r
                else 0.r
            }

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(256) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-11)
                }
            }
        }
    }

    describe("Given 1024 length array of values which do not matter") {
        val x = Array(1024) { 0.r }.asSequence()

        arrayOf(-4, -1, 0, 6, 120, 511, 513, 1023).forEach { n ->
            describe("FFT can be calculated only for N which is power of 2. Checking $n") {
                val e = catch { fft(x, n) }
                it("should throw an exception of ${IllegalArgumentException::class}") {
                    assertThat(e)
                            .isNotNull()
                            .isInstanceOf(IllegalArgumentException::class)
                            .hasMessage("N should be power of 2 but $n found")
                }
            }
        }
    }
})