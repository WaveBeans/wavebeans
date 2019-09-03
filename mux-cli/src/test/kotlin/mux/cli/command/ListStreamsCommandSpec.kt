package mux.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.cli.OutputDescriptor
import mux.cli.Session
import mux.cli.scope.RootScope
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianInput
import mux.lib.stream.FiniteInputSampleStream
import mux.lib.stream.plus
import mux.lib.stream.sampleStreamWithZeroFilling
import mux.lib.stream.sine
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.xdescribe

object ListStreamsCommandSpec : Spek({
    fun newSession() = Session(OutputDescriptor(50.0f, BitDepth.BIT_8))

    xdescribe("Session with no streams") {
        val session = newSession()

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 'No streams'") { assertThat(output).isEqualTo("No streams") }
        }
    }

    xdescribe("Generated sinusoid as the only one stream in the session") {
        val session = newSession()
        val input = 20.sine(amplitude = 0.1)
        session.registerSampleStream("stream1", input)

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("[stream1] 1.00 sec (Sinusoid amplitude=0.1, Sinusoid length=1.0sec, Sinusoid offset=0.0sec, Sinusoid frequency=20.0Hz)")
            }
        }
    }

    xdescribe("Byte array sourced 8bit as the only one stream in the session") {
        val session = newSession()
        val input = ByteArrayLittleEndianInput(
                50.0f,
                BitDepth.BIT_8,
                ByteArray(100) { 0 }
        )
        session.registerSampleStream("stream1", FiniteInputSampleStream(input).sampleStreamWithZeroFilling())

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("[stream1] 2.00 sec (Bit depth=8 bit, Size=100 bytes)")
            }
        }
    }

    xdescribe("Mixed as the only one stream in the session") {
        val session = newSession()
        val samples1 = FiniteInputSampleStream(ByteArrayLittleEndianInput(
                50.0f,
                BitDepth.BIT_8,
                ByteArray(100) { 0 }
        )).sampleStreamWithZeroFilling()
        val samples2 = 20.sine(amplitude = 0.1, timeOffset = 1.0)
        val mix = samples1 + samples2
        session.registerSampleStream("stream1", mix)

        describe("Listing all streams") {
            val command = ListStreamsCommand(session)
            val output = command.run(RootScope(session), null)

            it("should return 1 stream with name and description") {
                assertThat(output).isEqualTo("[stream1] 2.00 sec ([Merging] Sinusoid amplitude=0.1, [Merging] Sinusoid " +
                        "length=1.0sec, [Merging] Sinusoid offset=0.0sec, [Merging] Sinusoid frequency=20.0Hz, " +
                        "[Source] Bit depth=8 bit, [Source] Size=100 bytes, Samples count=100, Shift=0)")
            }
        }
    }

    xdescribe("Byte array sourced 16bit and 32 bit as streams in the session") {
        val session = newSession()
        val input1 = ByteArrayLittleEndianInput(
                50.0f,
                BitDepth.BIT_16,
                ByteArray(100) { 0 }
        )
        session.registerSampleStream("stream1", FiniteInputSampleStream(input1).sampleStreamWithZeroFilling())
        val input2 = ByteArrayLittleEndianInput(
                50.0f,
                BitDepth.BIT_32,
                ByteArray(100) { 0 }
        )
        session.registerSampleStream("stream2", FiniteInputSampleStream(input2).sampleStreamWithZeroFilling())

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