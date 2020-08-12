package io.wavebeans.lib.stream

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.seqStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object ConcatenatedStreamSpec : Spek({

    describe("Concatenate finite streams") {

        it("should concatenate two non empty streams") {
            val s1 = seqStream().trim(100)
            val s2 = seqStream().trim(100)
            val r = s1..s2
            assertThat(r).all {
                isInstanceOf(FiniteStream::class)
                lengthInMs().isEqualTo(200L)
                sequence(1000.0f).eachIndexed(200) { sample, idx ->
                    val sampleValue = (idx % 100) * 1e-10
                    sample.isCloseTo(sampleValue, 1e-20)
                }
            }
        }
        it("should concatenate when first stream is empty") {
            val s1 = seqStream().trim(0)
            val s2 = seqStream().trim(100)
            val r = s1..s2
            assertThat(r).all {
                isInstanceOf(FiniteStream::class)
                lengthInMs().isEqualTo(100L)
                sequence(1000.0f).eachIndexed(100) { sample, idx ->
                    val sampleValue = (idx % 100) * 1e-10
                    sample.isCloseTo(sampleValue, 1e-20)
                }
            }
        }
        it("should concatenate when second stream is empty") {
            val s1 = seqStream().trim(100)
            val s2 = seqStream().trim(0)
            val r = s1..s2
            assertThat(r).all {
                isInstanceOf(FiniteStream::class)
                lengthInMs().isEqualTo(100L)
                sequence(1000.0f).eachIndexed(100) { sample, idx ->
                    val sampleValue = (idx % 100) * 1e-10
                    sample.isCloseTo(sampleValue, 1e-20)
                }
            }
        }
        it("should concatenate five non empty streams") {
            val s1 = seqStream().trim(100)
            val s2 = seqStream().trim(100)
            val s3 = seqStream().trim(100)
            val s4 = seqStream().trim(100)
            val s5 = seqStream().trim(100)
            val r = s1..s2..s3..s4..s5
            assertThat(r).all {
                isInstanceOf(FiniteStream::class)
                lengthInMs().isEqualTo(500L)
                sequence(1000.0f).eachIndexed(500) { sample, idx ->
                    val sampleValue = (idx % 100) * 1e-10
                    sample.isCloseTo(sampleValue, 1e-20)
                }
            }
        }
    }

    describe("Concatenate finite and infinite streams") {

        it("should concatenate two non empty streams") {
            val s1 = seqStream().trim(100)
            val s2 = seqStream()
            val r = s1..s2
            assertThat(r).all {
                isInstanceOf(BeanStream::class)
                sequence(1000.0f, take = 200).eachIndexed(200) { sample, idx ->
                    val sampleValue = (idx % 100) * 1e-10
                    sample.isCloseTo(sampleValue, 1e-20)
                }
            }
        }
        it("should concatenate empty finite stream with non-empty infinite one") {
            val s1 = seqStream().trim(0)
            val s2 = seqStream()
            val r = s1..s2
            assertThat(r).all {
                isInstanceOf(BeanStream::class)
                sequence(1000.0f, take = 200).eachIndexed(200) { sample, idx ->
                    val sampleValue = (idx % 200) * 1e-10
                    sample.isCloseTo(sampleValue, 1e-20)
                }
            }
        }
    }
})

private fun Assert<FiniteStream<Sample>>.lengthInMs() =
        prop("length") { it.length(TimeUnit.MILLISECONDS) }

private fun Assert<BeanStream<Sample>>.sequence(sampleRate: Float, take: Int = Int.MAX_VALUE) =
        prop("asSequence($sampleRate)") { it.asSequence(sampleRate).take(take).toList() }