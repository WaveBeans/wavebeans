package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.itShouldHave
import io.wavebeans.lib.rangeProjection
import io.wavebeans.lib.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object FiniteSampleStreamSpec : Spek({
    val sampleRate = 50.0f
    describe("Stream of 8 bit fs=50Hz as ByteArray LE input") {
        val stream = (0..99).stream(sampleRate, BitDepth.BIT_8).trim(2000)

        it("should have length 2s") { assertThat(stream.length(TimeUnit.SECONDS)).isEqualTo(2L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(sampleRate)).isEqualTo(1000L) }
        }

    }

    describe("Stream of 16 bit fs=50Hz as ByteArray LE input") {
        val stream = (0..199).stream(sampleRate, BitDepth.BIT_16).trim(2000)


        itShouldHave("Length should be 2000ms for sample rate 50Hz") { assertThat(stream.length()).isEqualTo(2000L) }

        describe("Projected range 0.0s..1.0s") {
            val projection = stream.rangeProjection(0, 1000, TimeUnit.MILLISECONDS)

            itShouldHave("Length should be 1000ms for sample rate 50Hz") { assertThat(projection.length(sampleRate)).isEqualTo(1000L) }
        }

    }
})