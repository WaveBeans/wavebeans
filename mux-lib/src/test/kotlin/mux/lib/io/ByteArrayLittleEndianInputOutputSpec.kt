package mux.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.BitDepth
import mux.lib.itShouldHave
import mux.lib.listOfBytesAsInts
import mux.lib.stream.AudioSampleStream
import mux.lib.stream.Sample
import mux.lib.stream.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

object ByteArrayLittleEndianInputOutputSpec : Spek({
    val sampleRate = 50.0f
    val buffer = ByteArray(100) { (it and 0xFF).toByte() }
    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 8, mono") {
        val input = ByteArrayLittleEndianAudioInput(
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
            val output = ByteArrayLittleEndianAudioOutput(
                    sampleRate,
                    BitDepth.BIT_8,
                    AudioSampleStream(input)
            )

            it("should return byte array the same as initial buffer") {
                assertThat(output.toByteArray()).isEqualTo(buffer)
            }
            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.getInputStream().readBytes()).isEqualTo(buffer)
            }
        }

        describe("Projected range 0.0s..1.0s") {
            val projection = input.rangeProjection(0, 1000, MILLISECONDS)

            itShouldHave("number of bytes 50") { assertThat(projection.size()).isEqualTo(50) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(1000L) }
            itShouldHave("Samples should be [0,50)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (0 until 50).toList()
                )
            }
        }

        describe("Projected range 0.5s..1.0s") {
            val projection = input.rangeProjection(500, 1000, MILLISECONDS)

            itShouldHave("number of bytes 25") { assertThat(projection.size()).isEqualTo(25) }
            itShouldHave("length 500ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(500L) }
            itShouldHave("samples with values [25,50)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (25 until 50).toList()
                )
            }
        }

        describe("Projected range 0.1s..0.2s") {
            val projection = input.rangeProjection(100, 200, MILLISECONDS)

            itShouldHave("number of bytes 5") { assertThat(projection.size()).isEqualTo(5) }
            itShouldHave("length 100ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(100L) }
            itShouldHave("samples with values [5,10)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (5 until 10).toList()
                )
            }
        }

        describe("Projected range 1.5s..2.5s") {
            val projection = input.rangeProjection(1500, 2500, MILLISECONDS)

            itShouldHave("number of bytes 25") { assertThat(projection.size()).isEqualTo(25) }
            itShouldHave("length 500ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(500L) }
            itShouldHave("samples with values [75,100)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (75 until 100).toList()
                )
            }
        }

        describe("Projected range -1.5s..2.5s") {
            val projection = input.rangeProjection(-1500, 2500, MILLISECONDS)

            itShouldHave("number of bytes 100") { assertThat(projection.size()).isEqualTo(100) }
            itShouldHave("length 2000ms for sample rate 50Hz") { assertThat(projection.length(MILLISECONDS)).isEqualTo(2000L) }
            itShouldHave("samples with values [0,100)") {
                assertThat(projection.listOfBytesAsInts(50.0f)).isEqualTo(
                        (0 until 100).toList()
                )
            }
        }
    }

    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 16, mono") {
        val input = ByteArrayLittleEndianAudioInput(
                sampleRate,
                BitDepth.BIT_16,
                buffer
        )

        describe("samples are with values 0..100") {
            val samples = (0 until 100 step 2).map { sampleOf((it + 1 and 0xFF shl 8 or (it and 0xFF)).toShort()) }
            it("should be the same") {
                assertThat(input.asSequence(sampleRate).toList()).isEqualTo(samples)
            }
        }

        describe("output based on that input") {
            val output = ByteArrayLittleEndianAudioOutput(
                    sampleRate,
                    BitDepth.BIT_16,
                    AudioSampleStream(input)
            )

            it("should return byte array the same as initial buffer") {
                assertThat(output.toByteArray()).isEqualTo(buffer)
            }
            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.getInputStream().readBytes()).isEqualTo(buffer)
            }
        }
    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 8 bit ") {
        val signal = (-100 until 100).toList()
        val output = ByteArrayLittleEndianAudioOutput(sampleRate, BitDepth.BIT_8, AudioSampleStream(
                object : AudioInput {
                    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): AudioInput =
                            throw UnsupportedOperationException()

                    override fun length(timeUnit: TimeUnit): Long = throw UnsupportedOperationException()

                    override fun size(): Int = signal.size

                    override fun asSequence(sampleRate: Float): Sequence<Sample> = signal.asSequence().map { sampleOf(it.toByte()) }

                    override fun info(namespace: String?): Map<String, String> = throw UnsupportedOperationException()

                }
        ))

        it("output should be equal to 16 byte array 0-0x0F") {
            assertThat(output.toByteArray().map { it.toInt() }).isEqualTo(signal)
        }
    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 16 bit ") {
        val signal = (-100 until 100).toList()
        val output = ByteArrayLittleEndianAudioOutput(sampleRate, BitDepth.BIT_16, AudioSampleStream(
                object : AudioInput {
                    override fun length(timeUnit: TimeUnit): Long = throw UnsupportedOperationException()

                    override fun size(): Int = signal.size

                    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): AudioInput =
                            throw UnsupportedOperationException()

                    override fun asSequence(sampleRate: Float): Sequence<Sample> = signal.asSequence().map { sampleOf(it.toShort()) }

                    override fun info(namespace: String?): Map<String, String> = throw UnsupportedOperationException()

                }
        ))

        it("output should be equal to 16 byte array 0-0x0F") {
            assertThat(output.toByteArray().map { it.toInt() and 0xFF }).isEqualTo(signal.map { listOf(it and 0xFF, it and 0xFF00 shr 8) }.flatten())
        }
    }

    describe("Output of ByteArray LE, sequence of -100:100, encoding to 24 bit ") {
        val signal = (-100 until 100).toList()
        val output = ByteArrayLittleEndianAudioOutput(sampleRate, BitDepth.BIT_24, AudioSampleStream(
                object : AudioInput {
                    override fun length(timeUnit: TimeUnit): Long = throw UnsupportedOperationException()

                    override fun size(): Int = signal.size

                    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): AudioInput =
                            throw UnsupportedOperationException()

                    override fun asSequence(sampleRate: Float): Sequence<Sample> = signal.asSequence().map { sampleOf(it) }

                    override fun info(namespace: String?): Map<String, String> = throw UnsupportedOperationException()

                }
        ))

        it("output should be equal to 16 byte array 0-0x0F") {
            assertThat(output.toByteArray().map { it.toInt() and 0xFF }).isEqualTo(signal.map { listOf(it and 0xFF, it and 0xFF00 shr 8, it and 0xFF0000 shr 16) }.flatten())
        }
    }

})