package mux.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.*

object SelectionSpec : Spek({
    describe("A wav audio file 44100Hz 16 bit 1 channel") {
        val sampleRate = 44100.0f

        describe("when selecting with input 0..44100") {
            val selection = Selection.parse(sampleRate, "0..44100")

            it("should return 0 as starting sample") {
                assertThat(selection.start.time(MILLISECONDS)).isEqualTo(0L)
            }
            it("should return 1 as end sample") {
                assertThat(selection.end.time(MILLISECONDS)).isEqualTo(1000L)
            }
        }

        describe("when selecting with input 22050..44100") {
            val selection = Selection.parse(sampleRate, "22050..44100")

            it("should return 10 as starting sample") {
                assertThat(selection.start.time(MILLISECONDS)).isEqualTo(500L)
            }
            it("should return 100 as end sample") {
                assertThat(selection.end.time(MILLISECONDS)).isEqualTo(1000L)
            }
        }

        describe("when selecting with input 1s..2s") {
            val selection = Selection.parse(sampleRate, "1s..2s")

            it("should return valid starting sample") {
                assertThat(selection.start.time(MILLISECONDS)).isEqualTo(1000L)
            }
            it("should return valid end sample") {
                assertThat(selection.end.time(MILLISECONDS)).isEqualTo(2000L)
            }
        }

        describe("when selecting with input 1.000s..2.000s") {
            val selection = Selection.parse(sampleRate, "1.000s..2.000s")

            it("should return valid starting sample") {
                assertThat(selection.start.time(MILLISECONDS)).isEqualTo(1000L)
            }
            it("should return valid end sample") {
                assertThat(selection.end.time(MILLISECONDS)).isEqualTo(2000L)
            }
        }

        describe("when selecting with input 0.001s..1.001s") {
            val selection = Selection.parse(sampleRate, "0.001s..1.001s")

            it("should return valid starting sample") {
                assertThat(selection.start.time(MILLISECONDS)).isEqualTo(1L)
            }
            it("should return valid end sample") {
                assertThat(selection.end.time(MILLISECONDS)).isEqualTo(1001L)
            }
        }

        describe("when selecting with input 1ms..1001ms") {
            val selection = Selection.parse(sampleRate, "1ms..1001ms")

            it("should return valid starting sample") {
                assertThat(selection.start.time(MILLISECONDS)).isEqualTo(1L)
            }
            it("should return valid end sample") {
                assertThat(selection.end.time(MILLISECONDS)).isEqualTo(1001L)
            }
        }
    }
})