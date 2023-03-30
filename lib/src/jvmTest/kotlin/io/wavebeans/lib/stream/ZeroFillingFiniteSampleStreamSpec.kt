package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.asInt
import io.wavebeans.lib.repeat
import io.wavebeans.lib.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ZeroFillingFiniteSampleStreamSpec : Spek({

    fun stream(seq: List<Int>): FiniteStream<Sample> = object : FiniteStream<Sample> {
        override fun length(timeUnit: TimeUnit): Long = throw UnsupportedOperationException()

        override fun asSequence(sampleRate: Float): Sequence<Sample> {
            return seq.asSequence().map { sampleOf(it) }
        }

        override fun inputs(): List<AnyBean> = throw UnsupportedOperationException()

        override val parameters: BeanParams
            get() = throw UnsupportedOperationException()

        override val desiredSampleRate: Float? = null

        override fun samplesCount(): Long  = throw UnsupportedOperationException()
    }


    describe("Finite stream having 10 elements") {
        val seq = 10.repeat { it }
        val zeroFilling = stream(seq).stream(AfterFilling(ZeroSample))

        it("should return first 10 elements") {
            assertThat(
                    getZeroFillSeq(zeroFilling)
                            .take(10)
                            .toList()
            ).isEqualTo(seq)
        }

        it("should return 0 after first 10 elements") {
            assertThat(getZeroFillSeq(zeroFilling)
                    .drop(10)
                    .take(10)
                    .toList()
            ).isEqualTo(10.repeat { 0 })
        }

    }

    describe("Finite stream having more elements than default sample array size") {
        val elCount = (512 * 3.14).toInt()
        val seq = elCount.repeat { it }
        val zeroFilling = stream(seq).stream(AfterFilling(ZeroSample))

        it("should return first $elCount elements") {
            assertThat(
                    getZeroFillSeq(zeroFilling)
                            .take(elCount)
                            .toList()
            ).isEqualTo(seq)
        }

        it("should return 0 after first $elCount elements") {
            assertThat(getZeroFillSeq(zeroFilling)
                    .drop(elCount)
                    .take(10)
                    .toList()
            ).isEqualTo(10.repeat { 0 })
        }

    }

    describe("Finite stream having 10 elements taking first half") {
        val seq = 10.repeat { it }
        val zeroFilling = stream(seq).stream(AfterFilling(ZeroSample)).rangeProjection(0, 500)

        it("should return first 5 elements") {
            assertThat(
                    getZeroFillSeq(zeroFilling)
                            .take(5)
                            .toList()
            ).isEqualTo(seq.take(5))
        }

        it("should return nothing after first 5 elements") {
            assertThat(
                    getZeroFillSeq(zeroFilling)
                            .drop(5)
                            .take(10)
                            .toList()
            ).isEmpty()
        }

    }

    describe("Finite stream having 10 elements taking second half") {
        val seq = 10.repeat { it }
        val zeroFilling = stream(seq).stream(AfterFilling(ZeroSample)).rangeProjection(500, 1000)

        it("should return second 5 elements") {
            assertThat(
                    getZeroFillSeq(zeroFilling)
                            .take(5)
                            .toList()
            ).isEqualTo(seq.drop(5).take(5))
        }

        it("should return nothing after first 5 elements") {
            assertThat(
                    getZeroFillSeq(zeroFilling)
                            .drop(5)
                            .take(10)
                            .toList()
            ).isEmpty()
        }

    }
})

private fun getZeroFillSeq(zeroFilling: BeanStream<Sample>) =
        zeroFilling.asSequence(10.0f).map { it.asInt() }