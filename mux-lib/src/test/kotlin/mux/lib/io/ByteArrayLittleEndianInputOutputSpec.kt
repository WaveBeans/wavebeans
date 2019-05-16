package mux.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.BitDepth
import mux.lib.stream.AudioSampleStream
import mux.lib.stream.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ByteArrayLittleEndianInputOutputSpec : Spek({
    val sampleRate = 50.0f
    val buffer = ByteArray(100) { (it and 0xFF).toByte() }
    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 8, mono") {
        val input = ByteArrayLittleEndianAudioInput(
                BitDepth.BIT_8,
                buffer
        )

        describe("samples are with values 0..100") {
            val samples = (0 until 100).map { sampleOf(it.toByte()) }
            it("should be the same") {
                assertThat(input.asSequence().toList()).isEqualTo(samples)
            }
        }

        describe("output based on that input") {
            val output = ByteArrayLittleEndianAudioOutput(
                    BitDepth.BIT_8,
                    AudioSampleStream(input, sampleRate)
            )

            it("should return byte array the same as initial buffer") {
                assertThat(output.toByteArray()).isEqualTo(buffer)
            }
            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.getInputStream().readBytes()).isEqualTo(buffer)
            }
        }
    }

    describe("Wav LE input, sample rate = 50.0Hz, bit depth = 16, mono") {
        val input = ByteArrayLittleEndianAudioInput(
                BitDepth.BIT_16,
                buffer
        )

        describe("samples are with values 0..100") {
            val samples = (0 until 100 step 2).map { sampleOf((it + 1 and 0xFF shl 8 or (it and 0xFF)).toShort()) }
            it("should be the same") {
                assertThat(input.asSequence().toList()).isEqualTo(samples)
            }
        }

        describe("output based on that input") {
            val output = ByteArrayLittleEndianAudioOutput(
                    BitDepth.BIT_16,
                    AudioSampleStream(input, sampleRate)
            )

            it("should return byte array the same as initial buffer") {
                assertThat(output.toByteArray()).isEqualTo(buffer)
            }
            it("should return byte array as input stream the same as initial buffer") {
                assertThat(output.getInputStream().readBytes()).isEqualTo(buffer)
            }
        }
    }

})