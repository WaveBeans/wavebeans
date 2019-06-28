package mux.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianAudioInput
import mux.lib.io.SineGeneratedInput
import mux.lib.itShouldHave
import mux.lib.listOfBytesAsInts
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object AudioSampleStreamSpec : Spek({
    describe("Stream of 8 bit fs=50Hz as ByteArray LE input") {
        val stream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(50.0f, BitDepth.BIT_8, ByteArray(100) { it.toByte() })
        )

        it("should have length 2s") { assertThat(stream.length(50.0f, TimeUnit.SECONDS)).isEqualTo(2L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("number of samples 50") { assertThat(projection.samplesCount()).isEqualTo(50) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(50.0f)).isEqualTo(1000L) }
            itShouldHave("Length should be 500ms for sample rate 100Hz") { assertThat(projection.length(100.0f)).isEqualTo(500L) }
        }

    }

    describe("Stream of 16 bit fs=50Hz as ByteArray LE input") {
        val stream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(50.0f, BitDepth.BIT_16, ByteArray(200) { it.toByte() })
        )

        itShouldHave("number of samples 100") { assertThat(stream.samplesCount()).isEqualTo(100) }
        itShouldHave("Length should be 2000ms for sample rate 50Hz") { assertThat(stream.length(50.0f)).isEqualTo(2000L) }
        itShouldHave("Length should be 1000ms for sample rate 100Hz") { assertThat(stream.length(100.0f)).isEqualTo(1000L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("number of samples 50") { assertThat(projection.samplesCount()).isEqualTo(50) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(50.0f)).isEqualTo(1000L) }
            itShouldHave("Length should be 500ms for sample rate 100Hz") { assertThat(projection.length(100.0f)).isEqualTo(500L) }
        }

    }

    describe("Stream of Sine input") {
        val stream = AudioSampleStream(
                SineGeneratedInput(44100.0f, 440.0, 0.4, 2.0)
        )

        it("should have length 2s") { assertThat(stream.length(44100.0f, TimeUnit.SECONDS)).isEqualTo(2L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("number of samples 44100") { assertThat(projection.samplesCount()).isEqualTo(44100) }
            itShouldHave("Length should be 1000ms for sample rate 44100Hz") { assertThat(projection.length(44100.0f)).isEqualTo(1000L) }
        }
    }
})