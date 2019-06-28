package mux.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.cli.scope.MixStreamCommand
import mux.lib.io.SineGeneratedInput
import mux.lib.stream.AudioSampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MixStreamCommandSpec : Spek({

    describe("Two streams of 0.5s and 1.0s lengths, same sample rate") {

        val command = {
            val session = Session()
            session.registerSampleStream("stream1", AudioSampleStream(SineGeneratedInput(44100.0f, 440.0, 0.5, 0.5)))
            session.registerSampleStream("stream2", AudioSampleStream(SineGeneratedInput(44100.0f, 880.0, 0.3, 1.0)))
            MixStreamCommand(session, "stream1")
        }

        describe("Mixing stream 2 with default parameters") {
            val scope = command().newScope("stream2")

            it("scope should be ${AudioStreamScope::class}") { assertThat(scope).isInstanceOf(AudioStreamScope::class) }
            val a = scope as AudioStreamScope
            it("length of the stream should be 1s") { assertThat(a.samples.samplesCount()).isEqualTo(44100 * 1) }
        }

        describe("Mixing stream 2 with 0.5s shift") {
            val scope = command().newScope("stream2 22050")

            val a = scope as AudioStreamScope
            it("length of the stream should be 1.5s") { assertThat(a.samples.samplesCount()).isEqualTo((44100 * 1.5).toInt()) }
        }


        describe("Mixing stream 2 with 2.0s shift") {
            val scope = command().newScope("stream2 88200")

            val a = scope as AudioStreamScope
            it("length of the stream should be 3.0s") { assertThat(a.samples.samplesCount()).isEqualTo(44100 * 3) }
        }

        describe("Mixing 0.5s of stream 2 with no shift") {
            val scope = command().newScope("stream2 0 0s..0.500s")

            val a = scope as AudioStreamScope
            it("length of the stream should be 0.5s") { assertThat(a.samples.samplesCount()).isEqualTo((44100 * 0.5).toInt()) }
        }

        describe("Mixing 0.5s of stream 2 with 0.5s shift") {
            val scope = command().newScope("stream2 22050 0s..0.500s")

            val a = scope as AudioStreamScope
            it("length of the stream should be 1.0s") { assertThat(a.samples.samplesCount()).isEqualTo(44100) }
        }

    }
})