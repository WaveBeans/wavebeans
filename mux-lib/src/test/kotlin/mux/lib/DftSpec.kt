package mux.lib

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.catch
import mux.lib.io.sine
import mux.lib.io.sineSweep
import mux.lib.math.i
import mux.lib.math.minus
import mux.lib.math.plus
import mux.lib.math.r
import mux.lib.stream.plus
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
                    v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                }
            }
        }

        describe("Calculating FFT") {
            val fft = fft(x, 4)

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(4) { v, i ->
                    v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
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
                    v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                }
            }
        }
    }

    describe("Given sinusoid 64Hz, sample rate 128Hz, 2seconds") {
        val sine = 64.sine(amplitude = 1.0, timeOffset = 2.0)
                .asSequence(128.0f).flatMap { it.asSequence().map { it.r } }.take(256)

        describe("Calculating FFT") {
            val fft = fft(sine, 256)

            val expected = Array(256) { if (it == 128) 256.r else 0.r }

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(256) { v, i ->
                    v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                }
            }
        }
    }

    describe("Given sinusoid 64Hz, sample rate 128Hz, 2seconds, amplitude=0.5") {
        val sine = 64.sine(amplitude = 0.5, timeOffset = 2.0)
                .asSequence(128.0f).flatMap { it.asSequence().map { it.r } }.take(256)

        describe("Calculating FFT") {
            val fft = fft(sine, 256)

            val expected = Array(256) { if (it == 128) 128.r else 0.r }

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(256) { v, i ->
                    v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                }
            }
        }
    }

    describe("Given sinusoids 32 and 64Hz, sample rate 128Hz, 2seconds") {
        val sine1 = 32.sine()
        val sine2 = 64.sine()
        val x = (sine1 + sine2)
                .asSequence(128.0f).flatMap { it.asSequence().map { it.r } }.take(256)

        describe("Calculating FFT") {
            val fft = fft(x, 256)

            val expected = Array(256) {
                if (it == 128) 256.r
                else if (it == 64 || it == 192) 128.r
                else 0.r
            }

            it("should be as specific array") {
                assertThat(fft.toList()).eachIndexed(256) { v, i ->
                    v.isCloseTo(expected[i], 1e-11 + 1e-11.i)
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

    describe("FFT zero padding") {
        describe("x with odd number of elements") {
            val x = arrayOf(1.r, 2.r, 3.r, 4.r, 5.r).asSequence()
            describe("padding by 8") {
                val padded = x.zeropad(5, 8)
                it("should be [3,4,5,0,0,0,1,2]") {
                    val expected = arrayOf(3.r, 4.r, 5.r, 0.r, 0.r, 0.r, 1.r, 2.r)
                    assertThat(padded.toList()).eachIndexed(8) { v, i ->
                        v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                    }
                }
            }

            describe("padding by 16") {
                val padded = x.zeropad(5, 16)
                it("should be [3,4,5,0 x 11 times,1,2]") {
                    val expected = arrayOf(3.r, 4.r, 5.r) + Array(11) { 0.r } + arrayOf(1.r, 2.r)
                    assertThat(padded.toList()).eachIndexed(16) { v, i ->
                        v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                    }
                }
            }
        }

        describe("x with even number of elements") {
            val x = arrayOf(1.r, 2.r, 3.r, 4.r).asSequence()
            describe("padding by 8") {
                val padded = x.zeropad(4, 8)
                it("should be [3,4,0,0,0,0,1,2]") {
                    val expected = arrayOf(3.r, 4.r, 0.r, 0.r, 0.r, 0.r, 1.r, 2.r)
                    assertThat(padded.toList()).eachIndexed(8) { v, i ->
                        v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                    }
                }
            }

            describe("padding by 16") {
                val padded = x.zeropad(5, 16)
                it("should be [3,4,0 x 12 times,1,2]") {
                    val expected = arrayOf(3.r, 4.r) + Array(12) { 0.r } + arrayOf(1.r, 2.r)
                    assertThat(padded.toList()).eachIndexed(16) { v, i ->
                        v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                    }
                }
            }
        }

        describe("x with not enough elements") {
            val x = arrayOf(1.r, 2.r, 3.r, 4.r).asSequence()
            describe("padding by 8") {
                val padded = x.zeropad(5, 8)
                it("should be [3,4,0,0,0,0,1,2]") {
                    val expected = arrayOf(3.r, 4.r, 0.r, 0.r, 0.r, 0.r, 1.r, 2.r)
                    assertThat(padded.toList()).eachIndexed(8) { v, i ->
                        v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                    }
                }
            }
        }

        describe("x with more elements then needed") {
            val x = arrayOf(1.r, 2.r, 3.r, 4.r, 5.r, 6.r, 7.r).asSequence()
            describe("padding by 8") {
                val padded = x.zeropad(5, 8)
                it("should be [3,4,5,0,0,0,1,2]") {
                    val expected = arrayOf(3.r, 4.r, 5.r, 0.r, 0.r, 0.r, 1.r, 2.r)
                    assertThat(padded.toList().take(8)).eachIndexed(8) { v, i ->
                        v.isCloseTo(expected[i], 1e-14 + 1e-14.i)
                    }
                }
            }
        }
    }

    describe("FFT with zero padded sample") {
        val x = (1..5).map { it.r }.asSequence()

        describe("padding by 8") {
            val fft = fft(x.zeropad(5, 8), 8)

            it("should be as specific array") {
                val expected = arrayOf(
                        15 + 0.i,
                        7.24264069 - 5.41421356.i,
                        -3 - 2.i,
                        -1.24264069 + 2.58578644.i,
                        3 + 0.i,
                        -1.24264069 - 2.58578644.i,
                        -3 + 2.i,
                        7.24264069 + 5.41421356.i
                )
                assertThat(fft.toList()).eachIndexed(expected.size) { v, i ->
                    v.isCloseTo(expected[i], 1e-8 + 1e-8.i)
                }
            }
        }

    }

    describe("IFFT") {
        fun sine(freq: Double) = freq.sine(0.5, timeOffset = 1.0)

        val signals = mapOf(
                "[1..4]" to { (1..4).map { it.r }.asSequence() },
                "sine 64Hz @ 128Hz" to {
                    64.sine().asSequence(128.0f).flatMap { it.asSequence().map { it.r } }.take(64)
                },
                "sine 64Hz @ 1280Hz" to {
                    64.sine().asSequence(1280.0f).flatMap { it.asSequence().map { it.r } }.drop(128).take(512)
                },
                "sine 440Hz @ 44100Hz" to {
                    sine(440.0).asSequence(44100.0f).flatMap { it.asSequence().map { it.r } }.drop(1024).take(2048)
                },
                "sines 440Hz+880Hz @ 44100Hz" to {
                    (sine(440.0) + sine(880.0))
                            .asSequence(44100.0f).flatMap { it.asSequence().map { it.r } }.drop(1024).take(2048)
                },
                "sines 440Hz+1200Hz @ 44100Hz" to {
                    (sine(440.0) + sine(1200.0))
                            .asSequence(44100.0f).flatMap { it.asSequence().map { it.r } }.drop(1024).take(4096)
                },
                "sines 440Hz+1200Hz+30Hz+123Hz+456Hz @ 44100Hz" to {
                    (sine(440.0) + sine(440.0) + sine(30.0) + sine(123.0) + sine(456.0))
                            .asSequence(44100.0f).flatMap { it.asSequence().map { it.r } }.drop(1024).take(4096)
                },
                "sines [440..660]Hz @ 44100Hz" to {
                    (440..660)
                            .fold(sine(440.0)) { a, v -> a + sine(v.toDouble()) }
                            .asSequence(44100.0f).flatMap { it.asSequence().map { it.r } }.drop(1024).take(4096)
                },
                "sweep sine from 64Hz to 1024Hz @ 4096Hz" to {
                    (64..1024).sineSweep(0.5, 2.0)
                            .asSequence(4096.0f).flatMap { it.asSequence().map { it.r } }.drop(1024).take(4096)
                }
        )

        signals.forEach { signal ->
            describe("Given signal ${signal.key}") {
                println("Test case: ${signal.key}")
                var s = System.currentTimeMillis()
                val x = signal.value().toList().asSequence()
                println("Generation time = ${System.currentTimeMillis() - s}ms")
                val n = x.count()
                s = System.currentTimeMillis()
                val fft = fft(x, n)
                println("FFT time = ${System.currentTimeMillis() - s}ms")

                describe("Calculating inversed FFT") {
                    s = System.currentTimeMillis()
                    val ifft = fft(fft, n, inversed = true)
                    println("IFFT time = ${System.currentTimeMillis() - s}ms")

                    it("should be as source signal") {
                        val expected = x.toList()
                        assertThat(ifft.toList()).eachIndexed(expected.size) { v, i ->
                            v.isCloseTo(expected[i], 1e-12 + 1e-12.i)
                        }
                    }
                }
            }
        }
    }
})