package mux.lib.stream

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianAudioInput
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private fun SampleStream.asByteList(): List<Int> = this.asSequence(50.0f).map { it.asByte().toInt() and 0xFF }.toList()

object MixedSampleStreamSpec : Spek({

    describe("Source stream 50 samples length") {
        val sourceSampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        50.0f,
                        BitDepth.BIT_8,
                        ByteArray(50) { (it and 0xFF).toByte() }
                )
        )

        describe("Mixing in stream of 50 samples") {

            val sampleStream = AudioSampleStream(
                    ByteArrayLittleEndianAudioInput(
                            50.0f,
                            BitDepth.BIT_8,
                            ByteArray(50) { ((it + 50) and 0xFF).toByte() }
                    )
            )

            describe("On 0 position") {
                val mixed = sourceSampleStream.mixStream(0, sampleStream)

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should provide some info") { assertThat(mixed.info()).isNotEmpty() }

                it("should be 50 samples length") { assertThat(mixed.samplesCount()).isEqualTo(50) }
                it("should be array of values in range [50, 148] step 2") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            (50..148 step 2).toList()
                    )
                }

                describe("Getting sub-range [0,10)") {
                    val sub = mixed.rangeProjection(0, 9)

                    it("should be 10 sample length") { assertThat(sub.samplesCount()).isEqualTo(10) }
                    it("should be samples: [100,110)") {
                        assertThat(sub.asByteList()).isEqualTo(
                                (50..68 step 2).toList()
                        )
                    }
                }

                describe("Getting sub-range [0,110]") {
                    val e = catch { mixed.rangeProjection(0, 110) }

                    it("should throw an exception") { assertThat(e).isNotNull() }
                    it("should be the instance of ${SampleStreamException::class}") { assertThat(e!!).isInstanceOf(SampleStreamException::class) }
                    it("should have message pointing out the reason") { assertThat(e!!).hasMessage("End sample index is out of range") }
                }

                describe("Getting sub-range [100,110]") {
                    val e = catch { mixed.rangeProjection(100, 110) }

                    it("should throw an exception") { assertThat(e).isNotNull() }
                    it("should be the instance of ${SampleStreamException::class}") { assertThat(e!!).isInstanceOf(SampleStreamException::class) }
                    it("should have message pointing out the reason") { assertThat(e!!).hasMessage("Start sample index is out of range") }
                }

                describe("Getting sub-range [-1,-10]") {
                    val e = catch { mixed.rangeProjection(-1, 110) }

                    it("should throw an exception") { assertThat(e).isNotNull() }
                    it("should be the instance of ${SampleStreamException::class}") { assertThat(e!!).isInstanceOf(SampleStreamException::class) }
                    it("should have message pointing out the reason") { assertThat(e!!).hasMessage("Start sample index is out of range") }
                }

                describe("Getting sub-range [10,-2]") {
                    val e = catch { mixed.rangeProjection(10, -2) }

                    it("should throw an exception") { assertThat(e).isNotNull() }
                    it("should be the instance of ${SampleStreamException::class}") { assertThat(e!!).isInstanceOf(SampleStreamException::class) }
                    it("should have message pointing out the reason") { assertThat(e!!).hasMessage("End sample index should be greater than start sample index") }
                }
            }

            describe("On 10th index (10 samples shift)") {
                val mixed = sourceSampleStream.mixStream(10, sampleStream)

                it("should be 60 samples length") { assertThat(mixed.samplesCount()).isEqualTo(60) }
                it("should be array of values: [0, 10) + [60, 138] step 2 + [90, 100)") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            (0 until 10).toList() + (60..138 step 2).toList() + (90 until 100).toList()
                    )
                }
            }

            describe("On 50th position (50 samples shift)") {
                val mixed = sourceSampleStream.mixStream(50, sampleStream)

                it("should be 100 samples length") { assertThat(mixed.samplesCount()).isEqualTo(100) }
                it("should be array of values: [0, 50) + [50, 100)  ") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            ((0 until 50) + (50 until 100)).toList()
                    )
                }
            }

            describe("On 60th position") {
                val mixed = sourceSampleStream.mixStream(60, sampleStream)

                it("should be 110 samples length") { assertThat(mixed.samplesCount()).isEqualTo(110) }
                it("should be array of values: [0, 50) + 0.repeat(10) + [50, 100)  ") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            (0 until 50).toList() + (0 until 10).map { 0 }.toList() + (50 until 100).toList()
                    )
                }
            }

            describe("On negative position") {
                val e = catch { sourceSampleStream.mixStream(-1, sampleStream) }

                it("should throw an exception") { assertThat(e).isNotNull() }
                it("should be the instance of ${SampleStreamException::class}") { assertThat(e!!).isInstanceOf(SampleStreamException::class) }
                it("should have message pointing out the reason") { assertThat(e!!).hasMessage("Position can't be negative") }
            }


        }

        describe("Mixing in stream of 10 samples") {

            val sampleStream = AudioSampleStream(
                    ByteArrayLittleEndianAudioInput(
                            50.0f,
                            BitDepth.BIT_8,
                            ByteArray(10) { ((it + 50) and 0xFF).toByte() }
                    )
            )

            describe("On 0 position") {
                val mixed = sourceSampleStream.mixStream(0, sampleStream)

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should provide some info") { assertThat(mixed.info()).isNotEmpty() }

                it("should be 50 samples length") { assertThat(mixed.samplesCount()).isEqualTo(50) }
                it("should be array of values  [50, 68] step 2 + [10, 50)") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            ((50..68 step 2) + (10 until 50)).toList()
                    )
                }

            }

            describe("On 10th position (10 samples shift)") {
                val mixed = sourceSampleStream.mixStream(10, sampleStream)

                it("should be 50 samples length") { assertThat(mixed.samplesCount()).isEqualTo(50) }
                it("should be array of values  [0, 10) + [60, 78) step 2 + [20, 50)") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            ((0 until 10) + (60..78 step 2) + (20 until 50)).toList()
                    )
                }

            }

        }

        describe("Mixing in stream of 100 samples") {

            val sampleStream = AudioSampleStream(
                    ByteArrayLittleEndianAudioInput(
                            50.0f,
                            BitDepth.BIT_8,
                            ByteArray(100) { ((it + 50) and 0xFF).toByte() }
                    )
            )

            describe("On 0 position") {
                val mixed = sourceSampleStream.mixStream(0, sampleStream)

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should provide some info") { assertThat(mixed.info()).isNotEmpty() }

                it("should be 50 samples length") { assertThat(mixed.samplesCount()).isEqualTo(100) }
                it("should be array of values  [50, 148] step 2 + [50, 100)") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            ((50..148 step 2) + (100 until 150)).toList()
                    )
                }

            }

            describe("On 10 position") {
                val mixed = sourceSampleStream.mixStream(10, sampleStream)

                it("should be 50 samples length") { assertThat(mixed.samplesCount()).isEqualTo(110) }
                it("should be array of values  [0, 10) + [60, 138] step 2 + [90, 150)") {
                    assertThat(mixed.asByteList()).isEqualTo(
                            ((0 until 10) + (60..138 step 2) + (90 until 150)).toList()
                    )
                }

            }

        }
    }
})