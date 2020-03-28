package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FiniteInputStream
import io.wavebeans.lib.stream.FiniteStream
import io.wavebeans.lib.stream.rangeProjection
import io.wavebeans.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

private class ByteArrayFileOutputMock(
        val stream: FiniteStream<Sample>,
        val bitDepth: BitDepth
) : StreamOutput<Sample> {

    private val file = File.createTempFile("test", ".tmp").also { it.deleteOnExit() }

    override val input: Bean<Sample>
        get() = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

    override fun writer(sampleRate: Float): Writer {
        return object : ByteArrayLEFileOutputWriter(file.toURI(), stream, bitDepth, sampleRate) {
            override fun header(): ByteArray? = null

            override fun footer(): ByteArray? = null

        }
    }

    fun readBytes(sampleRate: Float): List<Byte> {
        val writer = writer(sampleRate)
        while (writer.write()) {
            sleep(0)
        }
        writer.close()
        return file.readBytes().toList()
    }
}

object ByteArrayLittleEndianInputOutputSpec : Spek({
    val sampleRate = 50.0f
    val buffer = ByteArray(100) { (it and 0xFF).toByte() }
    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 8, mono") {
        val input = buffer.asInput(sampleRate)

        describe("samples are with values 0..100") {
            val samples = (0 until 100).map { sampleOf(it.toByte()) }
            it("should be the same") {
                assertThat(input.asSequence(sampleRate).toList()).isEqualTo(samples)
            }
        }

        describe("output based on that input") {
            val output = ByteArrayFileOutputMock(
                    FiniteInputStream(input, NoParams()),
                    BitDepth.BIT_8
            )

            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.readBytes(sampleRate).map { it.asUnsignedByte() }).isEqualTo(buffer.map { it.toInt() and 0xFF })
            }
        }

        describe("Projected range 0.0s..1.0s") {
            val projection = input.rangeProjection(0, 1000, MILLISECONDS)

            itShouldHave("number of samples 50") { assertThat(projection.samplesCount(sampleRate)).isEqualTo(50L) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(sampleRate, MILLISECONDS)).isEqualTo(1000L) }
            itShouldHave("Samples should be [0,50)") {
                assertThat(projection.listOfBytesAsInts(sampleRate, 50)).isEqualTo(
                        (0 until 50).toList()
                )
            }
        }

        describe("Projected range 0.5s..1.0s") {
            val projection = input.rangeProjection(500, 1000, MILLISECONDS)

            itShouldHave("number of samples 25") { assertThat(projection.samplesCount(sampleRate)).isEqualTo(25L) }
            itShouldHave("length 500ms for sample rate 50Hz") { assertThat(projection.length(sampleRate, MILLISECONDS)).isEqualTo(500L) }
            itShouldHave("samples with values [25,50)") {
                assertThat(projection.listOfBytesAsInts(sampleRate)).isEqualTo(
                        (25 until 50).toList()
                )
            }
        }

        describe("Projected range 0.1s..0.2s") {
            val projection = input.rangeProjection(100, 200, MILLISECONDS)

            itShouldHave("number of samples 5") { assertThat(projection.samplesCount(sampleRate)).isEqualTo(5L) }
            itShouldHave("length 100ms for sample rate 50Hz") { assertThat(projection.length(sampleRate, MILLISECONDS)).isEqualTo(100L) }
            itShouldHave("samples with values [5,10)") {
                assertThat(projection.listOfBytesAsInts(sampleRate)).isEqualTo(
                        (5 until 10).toList()
                )
            }
        }

        describe("Projected range 1.5s..2.5s") {
            val projection = input.rangeProjection(1500, 2500, MILLISECONDS)

            itShouldHave("number of samples 25") { assertThat(projection.samplesCount(sampleRate)).isEqualTo(25L) }
            itShouldHave("length 500ms for sample rate 50Hz") { assertThat(projection.length(sampleRate, MILLISECONDS)).isEqualTo(500L) }
            itShouldHave("samples with values [75,100)") {
                assertThat(projection.listOfBytesAsInts(sampleRate)).isEqualTo(
                        (75 until 100).toList()
                )
            }
        }

        describe("Projected range -1.5s..2.5s") {
            val projection = input.rangeProjection(-1500, 2500, MILLISECONDS)

            itShouldHave("number of samples 100") { assertThat(projection.samplesCount(sampleRate)).isEqualTo(100L) }
            itShouldHave("length 2000ms for sample rate 50Hz") { assertThat(projection.length(sampleRate, MILLISECONDS)).isEqualTo(2000L) }
            itShouldHave("samples with values [0,100)") {
                assertThat(projection.listOfBytesAsInts(sampleRate)).isEqualTo(
                        (0 until 100).toList()
                )
            }
        }
    }

    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 16, mono") {
        val buffer16 = ByteArray(200) { (it and 0xFF).toByte() }
        val input = buffer16.asInput(sampleRate, BitDepth.BIT_16)

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
                    FiniteInputStream(input, NoParams()),
                    BitDepth.BIT_16
            )

            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.readBytes(sampleRate).toByteArray()).isEqualTo(buffer16)
            }
        }

        describe("Projected range 1.0s..2.0s") {
            val projection = input.rangeProjection(1000, 2000, MILLISECONDS)

            itShouldHave("number of samples 25") { assertThat(projection.samplesCount(sampleRate)).isEqualTo(50L) }
            itShouldHave("length 1000ms for sample rate 50Hz") { assertThat(projection.length(sampleRate, MILLISECONDS)).isEqualTo(1000L) }
            itShouldHave("samples with samples made of byte range [100,200)") {
                assertThat(projection.listOfShortsAsInts(sampleRate)).isEqualTo(
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
        val output = ByteArrayFileOutputMock(FiniteInputStream<Sample>(
                object : FiniteInput<Sample> {
                    override val parameters: BeanParams
                        get() = throw UnsupportedOperationException()

                    override fun length(timeUnit: TimeUnit): Long = Long.MAX_VALUE

                    override fun samplesCount(): Int = throw UnsupportedOperationException()

                    override fun asSequence(sampleRate: Float): Sequence<Sample> =
                            signal.asSequence().map { el -> sampleOf(el.toShort()) }

                },
                NoParams()
        ), BitDepth.BIT_16)

        it("output should be equal to 16 byte array 0-0x0F") {
            assertThat(output.readBytes(sampleRate).map { it.toInt() and 0xFF })
                    .isEqualTo(signal.map { listOf(it and 0xFF, it and 0xFF00 shr 8) }.flatten())
        }
    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 24 bit ") {
        val signal = (-100 until 100).toList()
        val output = ByteArrayFileOutputMock(FiniteInputStream<Sample>(
                object : FiniteInput<Sample> {
                    override val parameters: BeanParams
                        get() = throw UnsupportedOperationException()

                    override fun length(timeUnit: TimeUnit): Long = Long.MAX_VALUE

                    override fun samplesCount(): Int = throw UnsupportedOperationException()

                    override fun asSequence(sampleRate: Float): Sequence<Sample> =
                            signal.asSequence()
                                    .map { el -> sampleOf(el, true) }

                },
                NoParams()
        ), BitDepth.BIT_24)

        it("output should correspond to input signal") {
            assertThat(output.readBytes(sampleRate).map { it.toInt() and 0xFF })
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
            assertThat(output.readBytes(44100.0f).map { it.asUnsignedByte() })
                    .isEqualTo(
                            signal.asSequence(44100.0f).map { it.asByte().toInt() and 0xFF }.toList()
                    )
        }
    }

})