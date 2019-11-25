package io.wavebeans.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.lib.BitDepth
import mux.lib.NoParams
import mux.lib.io.ByteArrayLittleEndianInput
import mux.lib.io.ByteArrayLittleEndianInputParams
import mux.lib.io.sine
import mux.lib.stream.FiniteInputSampleStream
import mux.lib.stream.ZeroFilling
import mux.lib.stream.sampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.xdescribe

object InfoCommandSpec : Spek({

    fun newSession() = Session()

    xdescribe("InfoCommand within scope of 8-bit 50.0 Hz audio stream with 100 samples") {
        val sampleStream = FiniteInputSampleStream(
                ByteArrayLittleEndianInput(ByteArrayLittleEndianInputParams(
                        50.0f,
                        BitDepth.BIT_8,
                        ByteArray(100) { (it and 0xFF).toByte() }
                )),
                NoParams()
        ).sampleStream(ZeroFilling())

        val session = newSession()
        val scope = AudioStreamScope(session, "test-file.wav", sampleStream)
        val gen = InfoCommand(session, sampleStream)

        describe("During run generates info output") {
            val info = gen.run(scope, "")

            it("should be non null") { assertThat(info).isNotNull() }
            it("should be non empty") { assertThat(info!!).isNotEqualTo("") }
            it("should output bit depth") { assertThat(info!!).contains("Bit depth: 8 bit") }
            it("should output data size in bytes") { assertThat(info!!).contains("Size: 100 bytes") }
        }
    }

    xdescribe("InfoCommand within scope of generated sine") {
        val sampleStream = 10.sine()
        val session = newSession()
        val scope = AudioStreamScope(session, "test-file.wav", sampleStream)
        val gen = InfoCommand(session, sampleStream)

        describe("During run generates info output") {
            val info = gen.run(scope, "")

            it("should be non null") { assertThat(info).isNotNull() }
            it("should be non empty") { assertThat(info!!).isNotEqualTo("") }
            it("should output sinusoid frequency") { assertThat(info!!).contains("Sinusoid frequency: 10.0Hz") }
            it("should output sinusoid amplitude") { assertThat(info!!).contains("Sinusoid amplitude: 1.0") }
            it("should output sinusoid offset") { assertThat(info!!).contains("Sinusoid offset: 0.0sec") }
        }
    }

})
