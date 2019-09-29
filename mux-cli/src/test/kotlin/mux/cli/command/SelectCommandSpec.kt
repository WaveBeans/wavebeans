package mux.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import mux.cli.OutputDescriptor
import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.cli.scope.AudioSubStreamScope
import mux.lib.BitDepth
import mux.lib.NoParams
import mux.lib.io.ByteArrayLittleEndianInput
import mux.lib.io.ByteArrayLittleEndianInputParams
import mux.lib.stream.FiniteInputSampleStream
import mux.lib.stream.ZeroFilling
import mux.lib.stream.sampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.xdescribe

object SelectCommandSpec : Spek({
    xdescribe("Select command within 2 sec audio stream: ByteArray LE 8 bit fs=50.0") {
        val outputDescriptor = OutputDescriptor(50.0f, BitDepth.BIT_8)
        val session = Session(outputDescriptor)
        val audioStreamScope = AudioStreamScope(
                session,
                "stream1",
                FiniteInputSampleStream(
                        ByteArrayLittleEndianInput(ByteArrayLittleEndianInputParams(
                                outputDescriptor.sampleRate,
                                outputDescriptor.bitDepth,
                                ByteArray(100) { (it and 0xFF).toByte() }
                        ))
                        , NoParams()).sampleStream(ZeroFilling())
        )
        val command = SelectCommand(session, audioStreamScope)
        describe("Selecting new scope as 1s..2s") {
            val newScope = command.newScope("1s..2s")

            it("should be of ${AudioSubStreamScope::class}") { assertThat(newScope).isInstanceOf(AudioSubStreamScope::class) }
            it("should have prompt as `stream1<1.000s..2.000s>`") { assertThat(newScope.prompt()).isEqualTo("`stream1<1.000s..2.000s>`") }
            it("should be have stream listed in session under name `stream1<1.000s..2.000s>`") { assertThat(session.streamByName("stream1<1.000s..2.000s>")).isNotNull() }

            describe("Getting info on selected scope of 1s..2s") {
                val info = InfoCommand(session, (newScope as AudioStreamScope).samples).run(newScope, "")!!

                it("should has length of 1 second") { assertThat(info).contains("Length: 1000ms") }
                it("should has size of 88200 bytes") { assertThat(info).contains("Size: 50 bytes") }
            }

            describe("Selecting inner scope as 0.5s..0.8s") {
                val goingDeeperCommand = SelectCommand(session, newScope as AudioStreamScope)
                val innerScope = goingDeeperCommand.newScope("0.500s..0.800s")

                it("should be of ${AudioSubStreamScope::class}") { assertThat(innerScope).isInstanceOf(AudioSubStreamScope::class) }
                it("should have prompt as `stream1<1.000s..2.000s><0.500s..0.800s>`") { assertThat(innerScope.prompt()).isEqualTo("`stream1<1.000s..2.000s><0.500s..0.800s>`") }
                it("should be have stream listed in session under name `stream1<1.000s..2.000s><0.500s..0.800s>`") { assertThat(session.streamByName("stream1<1.000s..2.000s><0.500s..0.800s>")).isNotNull() }
            }
        }
    }

    xdescribe("Select command within 5 sec audio stream: ByteArray LE 16 bit fs=44100.0") {
        val outputDescriptor = OutputDescriptor(44100.0f, BitDepth.BIT_16)
        val session = Session(outputDescriptor)
        val audioStreamScope = AudioStreamScope(
                session,
                "stream1",
                FiniteInputSampleStream(
                        ByteArrayLittleEndianInput(ByteArrayLittleEndianInputParams(
                                outputDescriptor.sampleRate,
                                outputDescriptor.bitDepth,
                                ByteArray((outputDescriptor.sampleRate * 5).toInt()) { 0.toByte() }
                        ))
                        , NoParams()).sampleStream(ZeroFilling())
        )
        val command = SelectCommand(session, audioStreamScope)
        describe("Selecting new scope as 1s..2s") {
            val newScope = command.newScope("1s..2s")

            describe("Getting info on selected scope of 1s..2s") {
                val info = InfoCommand(session, (newScope as AudioStreamScope).samples).run(newScope, "")!!

                it("should has length of 1 second") { assertThat(info).contains("Length: 1000ms") }
                it("should has size of 88200 bytes") { assertThat(info).contains("Size: 88200 bytes") }
            }
        }
    }
})