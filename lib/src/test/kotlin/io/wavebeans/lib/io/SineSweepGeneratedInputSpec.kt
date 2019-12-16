package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isCloseTo
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.stream.rangeProjection
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS

object SineSweepGeneratedInputSpec : Spek({
    describe("Constant sine sweep of A=1.0, f1=10.0, f2=10.0, phi=1.0, fs=50.0 and t=0.1") {
        val generator = (10..10).sineSweep(
                1.0,
                time = 1.0,
                timeOffset = 1.0 / 10.0 / (Math.PI * 2.0) // 1.0 radians phase
        )

        val sample0 = 0.54030231
        val sample1 = -0.63332387
        val sample2 = -0.93171798
        val sample3 = 0.05749049
        val sample4 = 0.96724906
        val delta = 0.00001

        describe("generates sequence") {
            val seq = generator.asSequence(50.0f).take(5).toList()

            it("should be 5 samples array") {
                val expected = arrayOf(sample0, sample1, sample2, sample3, sample4)
                assertThat(seq).eachIndexed(expected.size) { a, idx ->
                    a.isCloseTo(expected[idx], delta)
                }
            }
        }

        xdescribe("projects a range 0..20ms") {
            val seq = generator.rangeProjection(0, 20, MILLISECONDS).asSequence(50.0f).take(1).toList()

            it("should be 1 sample array") {
                val expected = arrayOf(sample0)
                assertThat(seq).eachIndexed(expected.size) { a, idx ->
                    a.isCloseTo(expected[idx], delta)
                }
            }
        }

        xdescribe("projects a range 0..100ms") {
            val seq = generator.rangeProjection(0, 100, MILLISECONDS).asSequence(50.0f).take(5).toList()

            it("should be 5 sample array") {
                val expected = arrayOf(sample0, sample1, sample2, sample3, sample4)
                assertThat(seq).eachIndexed(expected.size) { a, idx ->
                    a.isCloseTo(expected[idx], delta)
                }
            }
        }

        xdescribe("projects a range -20..20ms") {
            val seq = generator.rangeProjection(-20, 20, MILLISECONDS).asSequence(50.0f).take(1).toList()

            it("should be 1 sample array") {
                val expected = arrayOf(sample0)
                assertThat(seq).eachIndexed(expected.size) { a, idx ->
                    a.isCloseTo(expected[idx], delta)
                }
            }
        }

        xdescribe("projects a range 20..40ms") {
            val seq = generator.rangeProjection(20, 40, MILLISECONDS).asSequence(50.0f).take(1).toList()

            it("should be 1 sample array") {
                val expected = arrayOf(sample1)
                assertThat(seq).eachIndexed(expected.size) { a, idx ->
                    a.isCloseTo(expected[idx], delta)
                }
            }
        }

        xdescribe("projects a range 80..120ms") {
            val seq = generator.rangeProjection(80, 120, MILLISECONDS).asSequence(50.0f).take(1).toList()

            it("should be 1 sample array") {
                val expected = arrayOf(sample4)
                assertThat(seq).eachIndexed(expected.size) { a, idx ->
                    a.isCloseTo(expected[idx], delta)
                }
            }
        }
    }
})
