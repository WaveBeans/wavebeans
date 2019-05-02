package mux.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.BitDepth
import mux.lib.WavLEAudioFileDescriptor
import mux.lib.io.ByteArrayLittleEndianAudioInput
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SampleStreamDownsamplingSpec : Spek({

    describe("A wav audio file 50Hz 8 bit 1 channel 2 seconds samplesCount with samples as integer monotonically growing, little endian") {
        val sampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        BitDepth.BIT_8,
                        ByteArray(100) { (it and 0xFF).toByte() }
                ),
                50.0f
        )

        describe("when down sampling to 25hz (2 times)") {
            val downSampledStream = sampleStream.downSample(2)

            it("samplesCount should remain 2 seconds") { assertThat(downSampledStream.length()).isEqualTo(2000) }
            it("sample rate should become 25hz") { assertThat(downSampledStream.sampleRate).isEqualTo(25.0f) }
            it("SampleStream.asSequence() should be sample array like [0, 2, 4, ...]") {
                val actual = downSampledStream.asSequence().toList()
                val expected = ByteArray(50) { (it * 2 and 0xFF).toByte() }.map { sampleOf(it) }.toList()
                assertThat(actual.map { it.asByte() }).isEqualTo(expected.map { it.asByte() })
            }
        }
    }

    describe("A wav audio file 50Hz 16 bit 1 channel 1 second samplesCount with samples as integer monotonically growing, little endian") {
        val sampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        BitDepth.BIT_16,
                        ByteArray(100) { (it and 0xFF).toByte() }
                ),
                50.0f
        )

        describe("when down sampling to 25hz (2 times)") {
            val downSampledStream = sampleStream.downSample(2)

            it("samplesCount should remain 1 second") { assertThat(downSampledStream.length()).isEqualTo(1000) }
            it("sample rate should become 25hz") { assertThat(downSampledStream.sampleRate).isEqualTo(25.0f) }
            it("SampleStream.asSequence() should be sample array like [0 or (1 << 8), 4 or (5 << 8), ...]") {
                val actual = downSampledStream.asSequence().toList()
                val expected = (0..24)
                        .map { idx -> (idx * 4 or (idx * 4 + 1 shl 8)).toShort() }
                        .map { sampleOf(it) }
                        .toList()
                assertThat(actual.map { it.asShort() }).isEqualTo(expected.map { it.asShort() })
            }
        }
    }

    describe("A wav audio file 50Hz 8 bit 1 channel 2 seconds samplesCount with samples as integer monotonically growing, little endian") {
        val sampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        BitDepth.BIT_8,
                        ByteArray(100) { (it and 0xFF).toByte() }
                ),
                50.0f
        )

        describe("when down sampling to 25hz (2 times)") {
            val downSampledStream = sampleStream.downSample(2)

            describe("when selecting 1st second of downsampled file") {
                val downSampledSubStream = downSampledStream.rangeProjection(0, 25)

                it("samples count should be 25") { assertThat(downSampledSubStream.samplesCount()).isEqualTo(25) }
                it("samplesCount should be 1 second") { assertThat(downSampledSubStream.length()).isEqualTo(1000) }
                it("sample rate should become 25hz") { assertThat(downSampledSubStream.sampleRate).isEqualTo(25.0f) }
                it("SampleStream.toByteArray() should be array like [0, 2, 4, ...]") {
                    val actual = downSampledSubStream.asSequence().toList()
                    val expected = ByteArray(25) { (it * 2 and 0xFF).toByte() }.map { sampleOf(it) }.toList()
                    assertThat(actual.map { it.asByte() }).isEqualTo(expected.map { it.asByte() })
                }
            }

            describe("when selecting 2nd second of downsampled file") {
                val downSampledSubStream = downSampledStream.rangeProjection(25, 50)

                it("samples count should be 25") { assertThat(downSampledSubStream.samplesCount()).isEqualTo(25) }
                it("samplesCount should be 1 second") { assertThat(downSampledSubStream.length()).isEqualTo(1000) }
                it("sample rate should become 25hz") { assertThat(downSampledSubStream.sampleRate).isEqualTo(25.0f) }
                it("SampleStream.toByteArray() should be array like [50, 52, 54, ...]") {
                    val actual = downSampledSubStream.asSequence().toList()
                    val expected = ByteArray(25) { ((it + 25) * 2 and 0xFF).toByte() }.map { sampleOf(it) }.toList()
                    assertThat(actual.map { it.asByte() }).isEqualTo(expected.map { it.asByte() })
                }
            }
        }
    }

})

