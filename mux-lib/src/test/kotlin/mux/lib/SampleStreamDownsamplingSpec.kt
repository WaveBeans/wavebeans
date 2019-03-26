package mux.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SampleStreamDownsamplingSpec : Spek({

    describe("A wav audio file 50Hz 8 bit 1 channel 2 seconds length with samples as integer monotonically growing") {
        val sampleStream = SampleByteArrayStream(
                ByteArray(100) { (it and 0xFF).toByte() },
                WavLEAudioFileDescriptor(50, 8, 1)
        )

        describe("when down sampling to 25hz (2 times)") {
            val downSampledStream = sampleStream.downSample(2)

            it("length should remain 2 seconds") { assertThat(downSampledStream.length()).isEqualTo(2000) }
            it("sample rate should become 25hz") { assertThat(downSampledStream.descriptor.sampleRate).isEqualTo(25) }
            it("data size should become 50 bytes") { assertThat(downSampledStream.dataSize()).isEqualTo(50) }
            it("SampleStream.toByteArray() should be array like [0, 2, 4, ...]") {
                val actual = downSampledStream.toByteArray().toList()
                val expected = ByteArray(50) { (it * 2 and 0xFF).toByte() }.toList()
                assertThat(actual).isEqualTo(expected)
            }
            it("SampleStream.getInputStream() should return sequence like [0, 2, 4, ...]") {
                val bb = ByteArray(downSampledStream.dataSize())
                downSampledStream.getInputStream().read(bb)
                val expected = ByteArray(50) { (it * 2 and 0xFF).toByte() }.toList()
                assertThat(bb.toList()).isEqualTo(expected)
            }
        }
    }

    describe("A wav audio file 50Hz 16 bit 1 channel 1 second length with samples as integer monotonically growing") {
        val sampleStream = SampleByteArrayStream(
                ByteArray(100) { (it and 0xFF).toByte() },
                WavLEAudioFileDescriptor(50, 16, 1)
        )

        describe("when down sampling to 25hz (2 times)") {
            val downSampledStream = sampleStream.downSample(2)

            it("length should remain 1 second") { assertThat(downSampledStream.length()).isEqualTo(1000) }
            it("sample rate should become 25hz") { assertThat(downSampledStream.descriptor.sampleRate).isEqualTo(25) }
            it("data size should become 50 bytes") { assertThat(downSampledStream.dataSize()).isEqualTo(50) }
            it("SampleStream.toByteArray() should be array like [0, 1, 4, 5, 8, 9, ...]") {
                val actual = downSampledStream.toByteArray().toList()
                val expected = (0..24)
                        .map { idx -> listOf(idx * 4, idx * 4 + 1).map { it.toByte() } }
                        .flatten()
                        .toList()
                assertThat(actual).isEqualTo(expected)
            }
            val expected = (0..24)
                    .map { idx -> listOf(idx * 4, idx * 4 + 1).map { it.toByte() } }
                    .flatten()
                    .toList()
            it("SampleStream.toByteArray() should be array like [0, 1, 4, 5, 8, 9, ...]") {
                assertThat(downSampledStream.toByteArray().toList()).isEqualTo(expected)
            }
            it("SampleStream.getInputStream() should return sequence like [0, 4, 8, ...]") {
                val bb = ByteArray(downSampledStream.dataSize())
                downSampledStream.getInputStream().read(bb)
                assertThat(bb.toList()).isEqualTo(expected)
            }
        }
    }

    describe("A wav audio file 50Hz 8 bit 1 channel 2 seconds length with samples as integer monotonically growing") {
        val sampleStream = SampleByteArrayStream(
                ByteArray(100) { (it and 0xFF).toByte() },
                WavLEAudioFileDescriptor(50, 8, 1)
        )

        describe("when down sampling to 25hz (2 times)") {
            val downSampledStream = sampleStream.downSample(2)

            describe("when selecting 1st second of downsampled file") {
                val downSampledSubStream = downSampledStream.rangeProjection(0, 25)

                it("samples count should be 25") { assertThat(downSampledSubStream.samplesCount()).isEqualTo(25) }
                it("length should be 1 second") { assertThat(downSampledSubStream.length()).isEqualTo(1000) }
                it("sample rate should become 25hz") { assertThat(downSampledSubStream.descriptor.sampleRate).isEqualTo(25) }
                it("data size should become 25 bytes") { assertThat(downSampledSubStream.dataSize()).isEqualTo(25) }
                it("SampleStream.toByteArray() should be array like [0, 2, 4, ...]") {
                    val actual = downSampledSubStream.toByteArray().toList()
                    val expected = ByteArray(25) { (it * 2 and 0xFF).toByte() }.toList()
                    assertThat(actual).isEqualTo(expected)
                }
                it("SampleStream.getInputStream() should return sequence like [0, 2, 4, ...]") {
                    val bb = ByteArray(downSampledSubStream.dataSize())
                    downSampledSubStream.getInputStream().read(bb)
                    val expected = ByteArray(25) { (it * 2 and 0xFF).toByte() }.toList()
                    assertThat(bb.toList()).isEqualTo(expected)
                }
            }

            describe("when selecting 2nd second of downsampled file") {
                val downSampledSubStream = downSampledStream.rangeProjection(25, 50)

                it("samples count should be 25") { assertThat(downSampledSubStream.samplesCount()).isEqualTo(25) }
                it("length should be 1 second") { assertThat(downSampledSubStream.length()).isEqualTo(1000) }
                it("sample rate should become 25hz") { assertThat(downSampledSubStream.descriptor.sampleRate).isEqualTo(25) }
                it("data size should become 25 bytes") { assertThat(downSampledSubStream.dataSize()).isEqualTo(25) }
                it("SampleStream.toByteArray() should be array like [50, 52, 54, ...]") {
                    val actual = downSampledSubStream.toByteArray().toList()
                    val expected = ByteArray(25) { ((it + 25) * 2 and 0xFF).toByte() }.toList()
                    assertThat(actual).isEqualTo(expected)
                }
                it("SampleStream.getInputStream() should return sequence like [50, 52, 54, ...]") {
                    val bb = ByteArray(downSampledSubStream.dataSize())
                    downSampledSubStream.getInputStream().read(bb)
                    val expected = ByteArray(25) { ((it + 25) * 2 and 0xFF).toByte() }.toList()
                    assertThat(bb.toList()).isEqualTo(expected)
                }
            }
        }
    }

})

