package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.*
import io.wavebeans.lib.io.sine
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import io.wavebeans.lib.TimeUnit.MILLISECONDS
import io.wavebeans.lib.TimeUnit.SECONDS

private fun Number.repeat(times: Int): List<Number> = (1..times).map { this }

class DiffSampleStreamSpec : Spek({
    val sampleRate = 50.0f
    describe("Two same size sequences") {
        val input1 = (1..8).stream(sampleRate, BitDepth.BIT_16)
        val input2 = (9..16).stream(sampleRate, BitDepth.BIT_16)

        describe("i2 - i1") {
            val diff = (input2 - input1).listOfShortsAsInts(sampleRate, 8)


            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be 8.repeat(8)") { assertThat(diff).isEqualTo(8.repeat(8)) }
        }

        describe("i1 - i2") {
            val diff = (input1 - input2).listOfShortsAsInts(sampleRate, 8)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be -8.repeat(8)") { assertThat(diff).isEqualTo((-8).repeat(8)) }
        }
    }


    describe("Second stream longer than first") {
        val input1 = (1..6).stream(sampleRate, BitDepth.BIT_16)
        val input2 = (9..16).stream(sampleRate, BitDepth.BIT_16)

        describe("i2 - i1") {
            val diff = (input2 - input1).listOfShortsAsInts(sampleRate, 8)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be 8.repeat(6) + 15..16") { assertThat(diff).isEqualTo(8.repeat(6) + (15..16)) }
        }

        describe("i1 - i2 @ 0") {
            val diff = (input1 - input2).listOfShortsAsInts(sampleRate, 8)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be -8.repeat(6) + -15..-16") { assertThat(diff).isEqualTo((-8).repeat(6) + (-15 downTo -16)) }
        }
    }

    describe("Two identical streams") {
        val i1 = (1..1024).stream(50.0f, BitDepth.BIT_16)
        val i2 = (1..1024).stream(50.0f, BitDepth.BIT_16)

        describe("i1 - i2 @ 0") {

            val r = (i1 - i2).listOfShortsAsInts(50.0f, 1024)
            it("should be 0.repeat(1024)") { assertThat(r).isEqualTo(0.repeat(1024)) }
        }

    }

    describe("sine(440)+sine(1000)-sine(440)") {
        val sineSampleRate = 44100.0f
        val r = 440.sine() +
                1000.sine() -
                440.sine()
        it("should be sine(1000)") {
            assertThat(r).isCloseTo(1000.sine(), sineSampleRate, 44100, 1e-14)

        }
    }

    describe("Two 2 seconds streams") {
        val i1 = (1..100).stream(50.0f, BitDepth.BIT_16)
        val i2 = (101..200).stream(50.0f, BitDepth.BIT_16)

        describe("i2 - i1 taking 1st second") {
            val diff = (i2 - i1)
                    .rangeProjection(0, 1, SECONDS)
                    .listOfShortsAsInts(50.0f, 50)

            it("should have length 50") { assertThat(diff.size).isEqualTo(50) }
            it("should be 100.repeat(50)") { assertThat(diff).isEqualTo(100.repeat(50)) }
        }

        describe("i2 - i1 taking 3rd 500ms") {
            val diff = (i2 - i1)
                    .rangeProjection(1000, 1500, MILLISECONDS)
                    .listOfShortsAsInts(50.0f, 25)

            it("should have length 25") { assertThat(diff.size).isEqualTo(25) }
            it("should be 100.repeat(25)") { assertThat(diff).isEqualTo(100.repeat(25)) }
        }
    }

})