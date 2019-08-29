package mux.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianInput
import mux.lib.itShouldHave
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object FiniteSampleStreamSpec : Spek({
    describe("Stream of 8 bit fs=50Hz as ByteArray LE input") {
        val stream = FiniteSampleStream(
                ByteArrayLittleEndianInput(50.0f, BitDepth.BIT_8, ByteArray(100) { it.toByte() })
        )

        it("should have length 2s") { assertThat(stream.length(TimeUnit.SECONDS)).isEqualTo(2L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("number of samples 50") { assertThat(projection.samplesCount()).isEqualTo(50) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length()).isEqualTo(1000L) }
        }

    }

    describe("Stream of 16 bit fs=50Hz as ByteArray LE input") {
        val stream = FiniteSampleStream(
                ByteArrayLittleEndianInput(50.0f, BitDepth.BIT_16, ByteArray(200) { it.toByte() })
        )

        itShouldHave("number of samples 100") { assertThat(stream.samplesCount()).isEqualTo(100) }
        itShouldHave("Length should be 2000ms for sample rate 50Hz") { assertThat(stream.length()).isEqualTo(2000L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("number of samples 50") { assertThat(projection.samplesCount()).isEqualTo(50) }
            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length()).isEqualTo(1000L) }
        }

    }
})