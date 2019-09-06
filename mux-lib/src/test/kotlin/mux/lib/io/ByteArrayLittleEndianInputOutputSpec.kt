package mux.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.*
import mux.lib.stream.FiniteInputSampleStream
import mux.lib.stream.FiniteSampleStream
import mux.lib.stream.sine
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

private class ByteArrayFileOutputMock(
        finiteSampleStream: FiniteSampleStream,
        bitDepth: BitDepth
) : ByteArrayLittleEndianFileOutput(URI.create("file:///dev/null"), finiteSampleStream, bitDepth) {
    override fun mux(): MuxNode = throw UnsupportedOperationException()

    override fun header(dataSize: Int): ByteArray? = throw UnsupportedOperationException()

    override fun footer(dataSize: Int): ByteArray? = throw UnsupportedOperationException()

    fun getInputStream(sampleRate: Float) = inputStream(sampleRate, null, null, MILLISECONDS)
}

object ByteArrayLittleEndianInputOutputSpec : Spek({
    val sampleRate = 50.0f
    val buffer = ByteArray(100) { (it and 0xFF).toByte() }
    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 8, mono") {
        val input = ByteArrayLittleEndianInput(
                sampleRate,
                BitDepth.BIT_8,
                buffer
        )

        describe("samples are with values 0..100") {
            val samples = (0 until 100).map { sampleOf(it.toByte()) }
            it("should be the same") {
                assertThat(input.asSequence(sampleRate).toList()).isEqualTo(samples)
            }
        }

        describe("output based on that input") {
            val output = ByteArrayFileOutputMock(
                    FiniteInputSampleStream(input),
                    BitDepth.BIT_8
            )

            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.getInputStream(sampleRate).readBytes().map { it.asUnsignedByte() }).isEqualTo(buffer.map { it.toInt() and 0xFF })
            }
        }

        describe("Projected range 0.0s..1.0s") {
            val projection = input.rangeProjection(0, 1000, MILLISECONDS)

            itShouldHave("number of samples 50") { assertThat(projection.samplesCount()).isEqualTo(50) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(1000L) }
            itShouldHave("Samples should be [0,50)") {
                assertThat(projection.listOfBytesAsInts(50.0f, 50)).isEqualTo(
                        (0 until 50).toList()
                )
            }
        }

        describe("Projected range 0.5s..1.0s") {
            val projection = input.rangeProjection(500, 1000, MILLISECONDS)

            itShouldHave("number of samples 25") { assertThat(projection.samplesCount()).isEqualTo(25) }
            itShouldHave("length 500ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(500L) }
            itShouldHave("samples with values [25,50)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (25 until 50).toList()
                )
            }
        }

        describe("Projected range 0.1s..0.2s") {
            val projection = input.rangeProjection(100, 200, MILLISECONDS)

            itShouldHave("number of samples 5") { assertThat(projection.samplesCount()).isEqualTo(5) }
            itShouldHave("length 100ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(100L) }
            itShouldHave("samples with values [5,10)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (5 until 10).toList()
                )
            }
        }

        describe("Projected range 1.5s..2.5s") {
            val projection = input.rangeProjection(1500, 2500, MILLISECONDS)

            itShouldHave("number of samples 25") { assertThat(projection.samplesCount()).isEqualTo(25) }
            itShouldHave("length 500ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(500L) }
            itShouldHave("samples with values [75,100)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (75 until 100).toList()
                )
            }
        }

        describe("Projected range -1.5s..2.5s") {
            val projection = input.rangeProjection(-1500, 2500, MILLISECONDS)

            itShouldHave("number of samples 100") { assertThat(projection.samplesCount()).isEqualTo(100) }
            itShouldHave("length 2000ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(2000L) }
            itShouldHave("samples with values [0,100)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (0 until 100).toList()
                )
            }
        }
    }

    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 16, mono") {
        val buffer16 = ByteArray(200) { (it and 0xFF).toByte() }
        val input = ByteArrayLittleEndianInput(
                sampleRate,
                BitDepth.BIT_16,
                buffer16
        )

        itShouldHave("number of samples 100") { assertThat(input.samplesCount()).isEqualTo(100) }
        itShouldHave("length 2000ms") { assertThat(input.length(MILLISECONDS)).isEqualTo(2000L) }

        describe("samples with samples made of byte range [0,200)") {
            val samples = (0 until 200 step 2)
                    .map { sampleOf((it + 1 and 0xFF shl 8 or (it and 0xFF)).toShort()) }
            it("should be the same") {
                assertThat(input.asSequence(sampleRate).toList()).isEqualTo(samples)
            }
        }

        describe("output based on that input") {
            val output = ByteArrayFileOutputMock(
                    FiniteInputSampleStream(input),
                    BitDepth.BIT_16
            )

            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.getInputStream(sampleRate).readBytes()).isEqualTo(buffer16)
            }
        }

        describe("Projected range 1.0s..2.0s") {
            val projection = input.rangeProjection(1000, 2000, MILLISECONDS)

            itShouldHave("number of samples 25") { assertThat(projection.samplesCount()).isEqualTo(50) }
            itShouldHave("length 1000ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(1000L) }
            itShouldHave("samples with samples made of byte range [100,200)") {
                assertThat(projection.listOfShortsAsInts(50.0f)).isEqualTo(
                        (100 until 200)
                                .windowed(2, 2)
                                .map { it[0] or (it[1] shl 8) }
                                .toList()
                )
            }
        }


    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 8 bit ") {
        // no need to test that case, 8 bit can't keep such signal as it is unsigned
    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 16 bit ") {
        val signal = (-100 until 100).toList()
        val output = ByteArrayFileOutputMock(FiniteInputSampleStream(
                object : FiniteInput {

                    override fun mux(): MuxNode = throw UnsupportedOperationException()

                    override fun length(timeUnit: TimeUnit): Long = Long.MAX_VALUE

                    override fun samplesCount(): Int =  throw UnsupportedOperationException()

                    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInput =
                            throw UnsupportedOperationException()

                    override fun asSequence(sampleRate: Float): Sequence<Sample> = signal.asSequence().map { sampleOf(it.toShort()) }

                }
        ), BitDepth.BIT_16)

        it("output should be equal to 16 byte array 0-0x0F") {
            assertThat(output.getInputStream(sampleRate).readBytes().map { it.toInt() and 0xFF }).isEqualTo(signal.map { listOf(it and 0xFF, it and 0xFF00 shr 8) }.flatten())
        }
    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 24 bit ") {
        val signal = (-100 until 100).toList()
        val output = ByteArrayFileOutputMock(FiniteInputSampleStream(
                object : FiniteInput {
                    override fun mux(): MuxNode = throw UnsupportedOperationException()

                    override fun length(timeUnit: TimeUnit): Long = Long.MAX_VALUE

                    override fun samplesCount(): Int = throw UnsupportedOperationException()

                    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInput =
                            throw UnsupportedOperationException()

                    override fun asSequence(sampleRate: Float): Sequence<Sample> = signal.asSequence().map { sampleOf(it, true) }

                }
        ), BitDepth.BIT_24)

        it("output should correspond to input signal") {
            assertThat(output.getInputStream(sampleRate).readBytes().map { it.toInt() and 0xFF })
                    .isEqualTo(signal
                            .map { listOf(it and 0xFF, it and 0xFF00 shr 8, it and 0xFF0000 shr 16) }
                            .flatten()
                    )
        }
    }

    describe("Output of ByteArray LE, sine 32 hz, encoding to 8 bit ") {
        val signal = 32.sine(.01, 0.5).trim(1000)
        val output = ByteArrayFileOutputMock(signal, BitDepth.BIT_8)

        it("output should be correspond to input signal") {
            assertThat(output.getInputStream(44100.0f).readBytes().map { it.asUnsignedByte() })
                    .isEqualTo(
                            signal.asSequence(44100.0f).map { it.asByte().toInt() and 0xFF }.toList()
                    )
        }
    }

})