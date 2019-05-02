package mux.cli.scope

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.BitDepth
import mux.lib.WavLEAudioFileDescriptor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SelectionSpec : Spek({
    describe("A wav audio file 44100Hz 16 bit 1 channel") {
        val descriptor = WavLEAudioFileDescriptor(44100.0f, BitDepth.BIT_16, 1)

        describe("when selecting with input 0..1") {
            val selection = Selection.parse("0..1")

            it("should return 0 as starting sample") {
                assertThat(selection.start.sampleIndex(descriptor)).isEqualTo(0)
            }
            it("should return 1 as end sample") {
                assertThat(selection.end.sampleIndex(descriptor)).isEqualTo(1)
            }
        }

        describe("when selecting with input 10..100") {
            val selection = Selection.parse("10..100")

            it("should return 10 as starting sample") {
                assertThat(selection.start.sampleIndex(descriptor)).isEqualTo(10)
            }
            it("should return 100 as end sample") {
                assertThat(selection.end.sampleIndex(descriptor)).isEqualTo(100)
            }
        }

        describe("when selecting with input 1s..2s") {
            val selection = Selection.parse("1s..2s")

            it("should return valid starting sample") {
                assertThat(selection.start.sampleIndex(descriptor)).isEqualTo(44100)
            }
            it("should return valid end sample") {
                assertThat(selection.end.sampleIndex(descriptor)).isEqualTo(44100 * 2)
            }
        }

        describe("when selecting with input 1.000s..2.000s") {
            val selection = Selection.parse("1.000s..2.000s")

            it("should return valid starting sample") {
                assertThat(selection.start.sampleIndex(descriptor)).isEqualTo(44100)
            }
            it("should return valid end sample") {
                assertThat(selection.end.sampleIndex(descriptor)).isEqualTo(44100 * 2)
            }
        }

        describe("when selecting with input 0.001s..1.001s") {
            val selection = Selection.parse("0.001s..1.001s")

            it("should return valid starting sample") {
                assertThat(selection.start.sampleIndex(descriptor)).isEqualTo(44)
            }
            it("should return valid end sample") {
                assertThat(selection.end.sampleIndex(descriptor)).isEqualTo(44100 + 44)
            }
        }

        describe("when selecting with input 1ms..1001ms") {
            val selection = Selection.parse("1ms..1001ms")

            it("should return valid starting sample") {
                assertThat(selection.start.sampleIndex(descriptor)).isEqualTo(44)
            }
            it("should return valid end sample") {
                assertThat(selection.end.sampleIndex(descriptor)).isEqualTo(44100 + 44)
            }
        }
    }
})