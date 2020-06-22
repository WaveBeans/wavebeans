package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameAs
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.listOfShortsAsInts
import io.wavebeans.lib.listOfShortsAsInts
import io.wavebeans.lib.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS

object SumSampleStreamSpec : Spek({

    describe("Source stream 50 samples length of 50 Hz of 8 bit (1 sec)") {
        val sourceSampleStream = (0..49).stream(50.0f, BitDepth.BIT_16)

        describe("Mixing in stream of 50 samples") {

            val sampleStream = (50..99).stream(50.0f, BitDepth.BIT_16)

            describe("On 0 position") {
                val mixed = sourceSampleStream + sampleStream

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }

                it("should be array of values in range [50, 148] step 2") {
                    assertThat(mixed.listOfShortsAsInts(50.0f, 50)).isEqualTo(
                            (50..148 step 2).toList()
                    )
                }

                describe("Getting projection of 0..200ms") {
                    val sub = mixed.rangeProjection(0, 200, MILLISECONDS)

                    it("should be samples: [50,68] step") {
                        assertThat(sub.listOfShortsAsInts(50.0f, 10)).isEqualTo(
                                (50..68 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of -200..200ms") {
                    val sub = mixed.rangeProjection(-200, 200, MILLISECONDS)

                    it("should be samples: [50,68] step") {
                        assertThat(sub.listOfShortsAsInts(50.0f, 10)).isEqualTo(
                                (50..68 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of 200..400ms") {
                    val sub = mixed.rangeProjection(200, 400, MILLISECONDS)

                    it("should be samples: [70,88] step 2") {
                        assertThat(sub.listOfShortsAsInts(50.0f, 10)).isEqualTo(
                                (70..88 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of 800..1000ms") {
                    val sub = mixed.rangeProjection(800, 1000, MILLISECONDS)

                    it("should be samples: [130,148] step 2") {
                        assertThat(sub.listOfShortsAsInts(50.0f, 10)).isEqualTo(
                                (130..148 step 2).toList()
                        )
                    }
                }

                describe("Getting projection of 800..1200ms") {
                    val sub = mixed.rangeProjection(800, 1200, MILLISECONDS)

                    it("should be samples: [130,148] step 2") {
                        assertThat(sub.listOfShortsAsInts(50.0f, 10)).isEqualTo(
                                (130..148 step 2).toList()
                        )
                    }
                }
            }
        }

        describe("Mixing in stream of 10 samples") {

            val sampleStream = (50..59).stream(50.0f, BitDepth.BIT_16)

            describe("On 0 position") {
                val mixed = sourceSampleStream + sampleStream

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should be array of values  [50, 68] step 2 + [10, 50)") {
                    assertThat(mixed.listOfShortsAsInts(50.0f, 50)).isEqualTo(
                            ((50..68 step 2) + (10 until 50)).toList()
                    )
                }

            }
        }

        describe("Mixing in stream of 100 samples") {

            val sampleStream = (50..149).stream(50.0f, BitDepth.BIT_16)

            describe("On 0 position") {
                val mixed = sourceSampleStream + sampleStream

                it("should be different instance from source stream") { assertThat(mixed).isNotSameAs(sourceSampleStream) }
                it("should be different instance from mixing in stream") { assertThat(mixed).isNotSameAs(sampleStream) }
                it("should be array of values  [50, 148] step 2 + [50, 100)") {
                    assertThat(mixed.listOfShortsAsInts(50.0f, 100)).isEqualTo(
                            ((50..148 step 2) + (100 until 150)).toList()
                    )
                }

            }
        }
    }
})