package mux.lib.stream

import assertk.Assert
import assertk.assertThat
import assertk.assertions.support.fail
import mux.lib.BitDepth
import mux.lib.asInt
import mux.lib.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


private fun Assert<List<Int>>.isProgression(expected: IntProgression) = given { actual ->
    if (actual == expected.toList()) return
    fail(expected.toList(), actual)
}


object ChangeAmplitudeSampleStreamSpec : Spek({
    describe("Int range stream") {
        val sampleRate = 10.0f
        fun stream(range: IntProgression) = range.stream(sampleRate, BitDepth.BIT_32)

        fun SampleStream.take(amount: Int) = this.asSequence(sampleRate)
                .flatMap { it.asSequence() }
                .map { it.asInt() }
                .take(amount)
                .toList()

        it("should do 2x change") {
            assertThat(stream(1..10).changeAmplitude(2.0).take(10)).isProgression(2..20 step 2)
        }
        it("should do 3x change") {
            assertThat(stream(1..10).changeAmplitude(3.0).take(10)).isProgression(3..30 step 3)
        }
        it("should do 0.5x change") {
            assertThat(stream(2..20 step 2).changeAmplitude(.5).take(10)).isProgression(1..10)
        }
        it("should do 0.33x change") {
            assertThat(stream(3..30 step 3).changeAmplitude(.33).take(10)).isProgression(1..10)
        }
        it("should do 2x change for the first half") {
            assertThat(stream(1..10).changeAmplitude(2.0).rangeProjection(0, 500).take(5)).isProgression(2..10 step 2)
        }
        it("should do 2x change for the second half") {
            assertThat(stream(1..10).changeAmplitude(2.0).rangeProjection(500, 1000).take(5)).isProgression(12..20 step 2)
        }
        it("should do 2x change for the middle part") {
            assertThat(stream(1..10).changeAmplitude(2.0).rangeProjection(200, 600).take(4)).isProgression(6..12 step 2)
        }
    }
})

