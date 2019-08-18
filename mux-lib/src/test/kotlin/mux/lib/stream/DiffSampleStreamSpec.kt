package mux.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.isCloseTo
import mux.lib.listOfBytesAsInts
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.*

private fun Number.repeat(times: Int): List<Number> = (1..times).map { this }

private fun SampleStream.listOfSignedBytesAsInts(sampleRate: Float): List<Int> =
        this.asSequence(sampleRate)
                .map { it.asByte().toInt() and 0xFF }
                .map { if (it > 128) it - 256 else it }
                .toList()


class DiffSampleStreamSpec : Spek({
    val sampleRate = 50.0f
    describe("Two same size sequences") {
        val input1 = (1..8).sampleStream(sampleRate)
        val input2 = (9..16).sampleStream(sampleRate)

        describe("i2 - i1 @ 0") {
            val diff = (input2 - input1).listOfSignedBytesAsInts(sampleRate)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be 8.repeat(8)") { assertThat(diff).isEqualTo(8.repeat(8)) }
        }

        describe("i1 - i2 @ 0") {
            val diff = (input1 - input2).listOfSignedBytesAsInts(sampleRate)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be -8.repeat(8)") { assertThat(diff).isEqualTo((-8).repeat(8)) }
        }

        describe("i2 - i1 @ 4") {
            val diff = diff(input2,
                    input1,
                    4
            ).listOfSignedBytesAsInts(sampleRate)

            it("should have length 12") { assertThat(diff.size).isEqualTo(12) }
            it("should be 9..12 + 12.repeat(4) + -5..-8") { assertThat(diff).isEqualTo((9..12) + 12.repeat(4) + (-5 downTo -8)) }
        }

        describe("i2 - i1 @ -4") {
            val diff = diff(
                    input2,
                    input1,
                    -4
            ).listOfSignedBytesAsInts(sampleRate)

            it("should have length 12") { assertThat(diff.size).isEqualTo(12) }
            it("should be -1..-4 + 4.repeat(4) + 13..16") { assertThat(diff).isEqualTo((-1 downTo -4) + (4).repeat(4) + (13..16)) }
        }
    }


    describe("Second stream longer than first") {
        val input1 = (1..6).sampleStream(sampleRate)
        val input2 = (9..16).sampleStream(sampleRate)

        describe("i2 - i1 @ 0") {
            val diff = diff(
                    input2,
                    input1
            ).listOfSignedBytesAsInts(sampleRate)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be 8.repeat(6) + 15..16") { assertThat(diff).isEqualTo(8.repeat(6) + (15..16)) }
        }

        describe("i1 - i2 @ 0") {
            val diff = diff(
                    input1,
                    input2
            ).listOfSignedBytesAsInts(sampleRate)

            it("should have length 8") { assertThat(diff.size).isEqualTo(8) }
            it("should be -8.repeat(6) + -15..-16") { assertThat(diff).isEqualTo((-8).repeat(6) + (-15 downTo -16)) }
        }

        describe("i1 - i2 @ 4") {
            val diff = diff(
                    input1,
                    input2,
                    4
            ).listOfSignedBytesAsInts(sampleRate)

            it("should have length 12") { assertThat(diff.size).isEqualTo(12) }
            it("should be 1..4 + -4.repeat(2) + -11..-16") { assertThat(diff).isEqualTo((1..4) + (-4).repeat(2) + (-11 downTo -16)) }
        }

        describe("i2 - i1 @ -4") {
            val diff = diff(
                    input2,
                    input1,
                    -4
            ).listOfSignedBytesAsInts(sampleRate)

            it("should have length 12") { assertThat(diff.size).isEqualTo(12) }
            it("should be -1..-4 + 4.repeat(2) + 11..16") { assertThat(diff).isEqualTo((-1 downTo -4) + (4).repeat(2) + (11..16)) }
        }
    }

    describe("Two identical streams") {
        val i1 = (1..1024).sampleStream(50.0f)
        val i2 = (1..1024).sampleStream(50.0f)

        describe("i1 - i2 @ 0") {

            val r = (i1 - i2).listOfSignedBytesAsInts(50.0f)
            it("should be 0.repeat(1024)") { assertThat(r).isEqualTo(0.repeat(1024)) }
        }

    }

    describe("sine(440)+sine(1000)-sine(440)") {
        val sineSampleRate = 44100.0f
        val r = 440.sine(1, sineSampleRate) +
                1000.sine(1, sineSampleRate) -
                440.sine(1, sineSampleRate)
        it("should be sine(1000)") {
            assertThat(r).isCloseTo(1000.sine(1, sineSampleRate), sineSampleRate, 1e-14)

        }
    }

    describe("Two 2 seconds streams") {
        val i1 = (1..100).sampleStream(50.0f)
        val i2 = (101..200).sampleStream(50.0f)

        describe("i2 - i1 @ 0 taking 1st second") {
            val diff = (i2 - i1)
                    .rangeProjection(0, 1, SECONDS)
                    .listOfSignedBytesAsInts(50.0f)

            it("should have length 50") { assertThat(diff.size).isEqualTo(50) }
            it("should be 100.repeat(50)") { assertThat(diff).isEqualTo(100.repeat(50)) }
        }

        describe("i2 - i1 @ 0 taking 3rd 500ms") {
            val diff = (i2 - i1)
                    .rangeProjection(1000, 1500, MILLISECONDS)
                    .listOfSignedBytesAsInts(50.0f)

            it("should have length 25") { assertThat(diff.size).isEqualTo(25) }
            it("should be 100.repeat(25)") { assertThat(diff).isEqualTo(100.repeat(25)) }
        }

        describe("i2 - i1 @ 25 taking first 500ms") {
            val diff = diff(
                    i2,
                    i1,
                    25
            )
                    .rangeProjection(0, 500, MILLISECONDS)
                    .listOfSignedBytesAsInts(50.0f)

            it("should have length 25") { assertThat(diff.size).isEqualTo(25) }
            it("should be 101..125") { assertThat(diff).isEqualTo((101..125).toList()) }
        }

        describe("i2 - i1 @ -25 taking last 500ms") {
            val s = diff(
                    i2,
                    i1,
                    -25
            )
            it("should have length 2.5s overall") { assertThat(s.length(50.0f)).isEqualTo(2500L) }
            val diff = s
                    .rangeProjection(2000, 2500, MILLISECONDS)
                    .listOfBytesAsInts(50.0f)

            it("should have length 25") { assertThat(diff.size).isEqualTo(25) }
            it("should be 176..200") { assertThat(diff).isEqualTo((176..200).toList()) }
        }
    }

})