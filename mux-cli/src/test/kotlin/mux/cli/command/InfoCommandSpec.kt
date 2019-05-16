package mux.cli.command

import assertk.assertThat
import assertk.assertions.*
import mux.cli.scope.AudioFileScope
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianAudioInput
import mux.lib.io.SineGeneratedInput
import mux.lib.stream.AudioSampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object InfoCommandSpec : Spek({

    describe("InfoCommand within scope of 8-bit 50.0 Hz audio stream with 100 samples") {
        val sampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        BitDepth.BIT_8,
                        ByteArray(100) { (it and 0xFF).toByte() }
                ),
                50.0f
        )
        val scope = AudioFileScope("test-file.wav", sampleStream)
        val gen = InfoCommand(sampleStream)

        describe("During run generates info output") {
            val info = gen.run(scope, "")

            it("should be non null") { assertThat(info).isNotNull() }
            it("should be non empty") { assertThat(info!!).isNotEqualTo("") }
            it("should output sample rate in Hz") { assertThat(info!!).contains("Sample rate: 50.0Hz") }
            it("should output bit depth") { assertThat(info!!).contains("Bit depth: 8 bit") }
            it("should output length in sec") { assertThat(info!!).contains("Length: 2.0s") }
            it("should output data size in bytes") { assertThat(info!!).contains("Size: 100 bytes") }
        }
    }

    describe("InfoCommand within scope of downsamples 2 times of 8-bit 50.0 Hz audio stream with 100 samples") {
        val sampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        BitDepth.BIT_8,
                        ByteArray(100) { (it and 0xFF).toByte() }
                ),
                50.0f
        )
        val scope = AudioFileScope("test-file.wav", sampleStream)
        val gen = InfoCommand(sampleStream.downSample(2))

        describe("During run generates info output") {
            val info = gen.run(scope, "")

            it("should be non null") { assertThat(info).isNotNull() }
            it("should be non empty") { assertThat(info!!).isNotEqualTo("") }
            it("should output sample rate in Hz") { assertThat(info!!).contains("Sample rate: 25.0Hz") }
            it("should output length in sec") { assertThat(info!!).contains("Length: 2.0s") }
            it("should output downsampling factor") { assertThat(info!!).contains("Downsampling factor: 2") }
            it("should output source sample rate") { assertThat(info!!).contains("[Source] Sample rate: 50.0Hz") }
            it("should output source data size") { assertThat(info!!).contains("[Source] Size: 100 bytes") }
            it("should output source bit depth") { assertThat(info!!).contains("[Source] Bit depth: 8 bit") }
        }
    }

    describe("InfoCommand within scope of generated sine") {
        val sampleStream = AudioSampleStream(
                SineGeneratedInput(
                        50.0f,
                        10.0,
                        1.0,
                        1.0
                ),
                50.0f
        )
        val scope = AudioFileScope("test-file.wav", sampleStream)
        val gen = InfoCommand(sampleStream)

        describe("During run generates info output") {
            val info = gen.run(scope, "")

            it("should be non null") { assertThat(info).isNotNull() }
            it("should be non empty") { assertThat(info!!).isNotEqualTo("") }
            it("should output sample rate in Hz") { assertThat(info!!).contains("Sample rate: 50.0Hz") }
            it("should output length in sec") { assertThat(info!!).contains("Length: 1.0s") }
            it("should output sinusoid frequency") { assertThat(info!!).contains("Sinusoid frequency: 10.0Hz") }
            it("should output sinusoid amplitude") { assertThat(info!!).contains("Sinusoid amplitude: 1.0") }
            it("should output sinusoid phase") { assertThat(info!!).contains("Sinusoid phase: 0.0") }
        }
    }

})