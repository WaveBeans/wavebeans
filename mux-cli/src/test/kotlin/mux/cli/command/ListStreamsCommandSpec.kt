package mux.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.cli.OutputDescriptor
import mux.cli.Session
import mux.cli.scope.RootScope
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianAudioInput
import mux.lib.io.SineGeneratedInput
import mux.lib.stream.AudioSampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ListStreamsCommandSpec : Spek({
    fun newSession() = Session(OutputDescriptor(50.0f, BitDepth.BIT_8))

    describe("Session with no streams") {
        val session = newSession()

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 'No streams'") { assertThat(output).isEqualTo("No streams") }
        }
    }

    describe("Generated sinusoid as the only one stream in the session") {
        val session = newSession()
        val input = SineGeneratedInput(50.0f, 20.0, 0.1, 1.0)
        session.registerSampleStream("stream1", AudioSampleStream(input))

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("[stream1] 1.00 sec (Sinusoid amplitude=0.1, Sinusoid length=1.0sec, Sinusoid offset=0.0sec, Sinusoid frequency=20.0Hz)")
            }
        }
    }

    describe("Byte array sourced 8bit as the only one stream in the session") {
        val session = newSession()
        val input = ByteArrayLittleEndianAudioInput(
                50.0f,
                BitDepth.BIT_8,
                ByteArray(100) { 0 }
        )
        session.registerSampleStream("stream1", AudioSampleStream(input))

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("[stream1] 2.00 sec (Bit depth=8 bit, Size=100 bytes)")
            }
        }
    }

    describe("Mixed as the only one stream in the session") {
        val session = newSession()
        val samples1 = AudioSampleStream(ByteArrayLittleEndianAudioInput(
                50.0f,
                BitDepth.BIT_8,
                ByteArray(100) { 0 }
        ))
        val samples2 = AudioSampleStream(SineGeneratedInput(50.0f, 20.0, 0.1, 1.0))
        val mix = samples1.mixStream(0, samples2)
        session.registerSampleStream("stream1", mix)

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("[stream1] 2.00 sec ([Mix-in] Sinusoid amplitude=0.1, [Mix-in] Sinusoid " +
                        "length=1.0sec, [Mix-in] Sinusoid offset=0.0sec, [Mix-in] Sinusoid frequency=20.0Hz, " +
                        "[Source] Bit depth=8 bit, [Source] Size=100 bytes, Samples count=100, Mix In position=0)")
            }
        }
    }

    describe("Byte array sourced 16bit and 32 bit as streams in the session") {
        val session = newSession()
        val input1 = ByteArrayLittleEndianAudioInput(
                50.0f,
                BitDepth.BIT_16,
                ByteArray(100) { 0 }
        )
        session.registerSampleStream("stream1", AudioSampleStream(input1))
        val input2 = ByteArrayLittleEndianAudioInput(
                50.0f,
                BitDepth.BIT_32,
                ByteArray(100) { 0 }
        )
        session.registerSampleStream("stream2", AudioSampleStream(input2))

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("""
                        [stream1] 1.00 sec (Bit depth=16 bit, Size=100 bytes)
                        [stream2] 0.50 sec (Bit depth=32 bit, Size=100 bytes)
                    """.trimIndent())
            }
        }
    }
})