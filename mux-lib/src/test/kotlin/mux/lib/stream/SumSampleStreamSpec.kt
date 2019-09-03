package mux.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameAs
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianInput
import mux.lib.listOfBytesAsInts
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS

object SumSampleStreamSpec : Spek({

    describe("Source stream 50 samples length of 50 Hz of 8 bit (1 sec)") {
        val sourceSampleStream = FiniteInputSampleStream(
                ByteArrayLittleEndianInput(
                        50.0f,
                        BitDepth.BIT_8,
                        ByteArray(50) { (it and 0xFF).toByte() }
                )
        ).sampleStream(ZeroFilling())

        describe("Mixing in stream of 50 samples") {

            val sampleStream = FiniteInputSampleStream(
                    ByteArrayLittleEndianInput(
                            50.0f,
                            BitDepth.BIT_8,
                            ByteArray(50) { ((it + 50) and 0xFF).toByte() }
                    )
            ).sampleStream(ZeroFilling())

            describe("On 0 position") {
                val mixed = sourceSampleStream + sampleStream

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }

                it("should be array of values in range [50, 148] step 2") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 50)).isEqualTo(
                            (50..148 step 2).toList()
                    )
                }

                describe("Getting projection of 0..200ms") {
                    val sub = mixed.rangeProjection(0, 200, MILLISECONDS)

                    it("should be samples: [50,68] step") {
                        assertThat(sub.listOfBytesAsInts(50.0f, 10)).isEqualTo(
                                (50..68 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of -200..200ms") {
                    val sub = mixed.rangeProjection(-200, 200, MILLISECONDS)

                    it("should be samples: [50,68] step") {
                        assertThat(sub.listOfBytesAsInts(50.0f, 10)).isEqualTo(
                                (50..68 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of 200..400ms") {
                    val sub = mixed.rangeProjection(200, 400, MILLISECONDS)

                    it("should be samples: [70,88] step 2") {
                        assertThat(sub.listOfBytesAsInts(50.0f, 10)).isEqualTo(
                                (70..88 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of 800..1000ms") {
                    val sub = mixed.rangeProjection(800, 1000, MILLISECONDS)

                    it("should be samples: [130,148] step 2") {
                        assertThat(sub.listOfBytesAsInts(50.0f, 10)).isEqualTo(
                                (130..148 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of 800..1200ms") {
                    val sub = mixed.rangeProjection(800, 1200, MILLISECONDS)

                    it("should be samples: [130,148] step 2") {
                        assertThat(sub.listOfBytesAsInts(50.0f, 10)).isEqualTo(
                                (130..148 step 2).toList()
                        )
                    }
                }
            }

            describe("On 10th index (10 samples shift)") {
                val mixed = sum(sourceSampleStream, sampleStream, 10)

                it("should be array of values: [0, 10) + [60, 138] step 2 + [90, 100)") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 60)).isEqualTo(
                            (0 until 10).toList() + (60..138 step 2).toList() + (90 until 100).toList()
                    )
                }
            }

            describe("On 50th position (50 samples shift)") {
                val mixed = sum(sourceSampleStream, sampleStream, 50)

                it("should be array of values: [0, 50) + [50, 100)  ") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 100)).isEqualTo(
                            ((0 until 50) + (50 until 100)).toList()
                    )
                }
            }

            describe("On 60th position") {
                val mixed = sum(sourceSampleStream, sampleStream, 60)

                it("should be array of values: [0, 50) + 0.repeat(10) + [50, 100)  ") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 110)).isEqualTo(
                            (0 until 50).toList() + (0 until 10).map { 0 }.toList() + (50 until 100).toList()
                    )
                }
            }

            describe("On -1 position") {
                val mixed = sum(sourceSampleStream, sampleStream, -1)

                it("should be array of values: [50] + [51..147] step 2 + [49]  ") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 51)).isEqualTo(
                            (listOf(50) + (51 .. 147 step 2) + listOf(49)).toList()
                    )
                }
            }


        }

        describe("Mixing in stream of 10 samples") {

            val sampleStream = FiniteInputSampleStream(
                    ByteArrayLittleEndianInput(
                            50.0f,
                            BitDepth.BIT_8,
                            ByteArray(10) { ((it + 50) and 0xFF).toByte() }
                    )
            ).sampleStream(ZeroFilling())

            describe("On 0 position") {
                val mixed = sourceSampleStream + sampleStream

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should be array of values  [50, 68] step 2 + [10, 50)") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 50)).isEqualTo(
                            ((50..68 step 2) + (10 until 50)).toList()
                    )
                }

            }

            describe("On 10th position (10 samples shift)") {
                val mixed = sum(sourceSampleStream, sampleStream, 10)
                it("should be array of values  [0, 10) + [60, 78) step 2 + [20, 50)") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 50 )).isEqualTo(
                            ((0 until 10) + (60..78 step 2) + (20 until 50)).toList()
                    )
                }

            }

        }

        describe("Mixing in stream of 100 samples") {

            val sampleStream = FiniteInputSampleStream(
                    ByteArrayLittleEndianInput(
                            50.0f,
                            BitDepth.BIT_8,
                            ByteArray(100) { ((it + 50) and 0xFF).toByte() }
                    )
            ).sampleStream(ZeroFilling())

            describe("On 0 position") {
                val mixed = sourceSampleStream + sampleStream

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should be array of values  [50, 148] step 2 + [50, 100)") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 100)).isEqualTo(
                            ((50..148 step 2) + (100 until 150)).toList()
                    )
                }

            }

            describe("On 10 position") {
                val mixed = sum(sourceSampleStream, sampleStream, 10)
                it("should be array of values  [0, 10) + [60, 138] step 2 + [90, 150)") {
                    assertThat(mixed.listOfBytesAsInts(50.0f, 110)).isEqualTo(
                            ((0 until 10) + (60..138 step 2) + (90 until 150)).toList()
                    )
                }

            }

        }
    }
})