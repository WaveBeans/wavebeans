package io.wavebeans.execution.pod

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PlainMedium
import io.wavebeans.execution.newTestStreamingPod
import io.wavebeans.lib.Sample
import io.wavebeans.lib.asInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe

object StreamingPodSpec : Spek({

    val modes = mapOf(
//            "Distributed" to ....
            "Multi-threaded" to Pair(
                    { ExecutionConfig.initForMultiThreadedProcessing() },
                    { list: List<Any>? -> list?.map { it as PlainMedium }?.flatMap { it.items.map { it as Sample } } }
            )
    )

    modes.forEach { mode ->
        describe("Mode: ${mode.key}") {
            beforeEachGroup { mode.value.first() }

            val sampleRemap = mode.value.second

            fun List<Any>?.toIntSamples(): List<Int>? = sampleRemap(this)?.map { it.asInt() }

            describe("StreamingPod returning predefined sequence") {
                val seq = (1..100).toList()

                describe("Iterate over pod sequence") {
                    val pod by memoized(mode = SCOPE) { newTestStreamingPod(seq) }

                    val iteratorKey by memoized(mode = SCOPE) { pod.iteratorStart(100.0f, 0) }

                    it("should be the same as defined sequence") {
                        assertThat(pod.iteratorNext(iteratorKey, 100).toIntSamples()).isEqualTo(seq)
                    }
                }

                describe("Iterate over pod sequence with 2 proxies") {
                    val pod by memoized(mode = SCOPE) { newTestStreamingPod(seq) }

                    val iteratorKeys by memoized(mode = SCOPE) {
                        arrayOf(
                                pod.iteratorStart(100.0f, 0),
                                pod.iteratorStart(100.0f, 0)
                        )
                    }

                    it("first should be the same as defined sequence") {
                        assertThat(pod.iteratorNext(iteratorKeys[0], 100).toIntSamples()).isEqualTo(seq)
                    }
                    it("second should be the same as defined sequence") {
                        assertThat(pod.iteratorNext(iteratorKeys[1], 100).toIntSamples()).isEqualTo(seq)
                    }
                }

                describe("Iterate over pod sequence for more than defined in sequence at once") {
                    val pod by memoized(mode = SCOPE) { newTestStreamingPod(seq) }

                    val iteratorKey1 by memoized(mode = SCOPE) { pod.iteratorStart(100.0f, 0) }

                    it("should be the same as defined sequence") {
                        assertThat(pod.iteratorNext(iteratorKey1, 101).toIntSamples()).isEqualTo(seq)
                    }
                }

                describe("Iterate over pod sequence for more than defined in sequence in two attempts") {
                    val pod by memoized(mode = SCOPE) { newTestStreamingPod(seq) }

                    val iteratorKey1 by memoized(mode = SCOPE) { pod.iteratorStart(100.0f, 0) }

                    it("first attempt should be the same as defined sequence") {
                        assertThat(pod.iteratorNext(iteratorKey1, 100).toIntSamples()).isEqualTo(seq)
                    }
                    it("second attempt should be null") {
                        assertThat(pod.iteratorNext(iteratorKey1, 1).toIntSamples()).isNull()
                    }
                }

                describe("Iterate over pod sequence in two attempts") {
                    val pod by memoized(mode = SCOPE) { newTestStreamingPod(seq) }

                    val iteratorKey1 by memoized(mode = SCOPE) { pod.iteratorStart(100.0f, 0) }

                    it("first attempt should be the same as defined sequence first half") {
                        assertThat(pod.iteratorNext(iteratorKey1, 50).toIntSamples()).isEqualTo(seq.take(50))
                    }
                    it("second attempt should be the same as defined sequence second half") {
                        assertThat(pod.iteratorNext(iteratorKey1, 50).toIntSamples()).isEqualTo(seq.drop(50).take(50))
                    }

                }

                describe("Iterate over pod sequence with two consumers. Partition size > 1") {
                    val pod by memoized(mode = SCOPE) { newTestStreamingPod(seq, partitionSize = 2) }
                    val keys by memoized(mode = SCOPE) {
                        arrayOf(
                                pod.iteratorStart(1.0f, 0),
                                pod.iteratorStart(1.0f, 0)
                        )
                    }

                    it("should generate correct output") {
                        val e1 = pod.iteratorNext(keys[0], 2).toIntSamples() ?: emptyList()
                        pod.iteratorNext(keys[1], 2).toIntSamples() ?: emptyList() // should read something

                        val e11 = pod.iteratorNext(keys[0], 2).toIntSamples() ?: emptyList()
                        val e12 = pod.iteratorNext(keys[0], 2).toIntSamples() ?: emptyList()
                        assertThat(e1 + e11 + e12).isEqualTo(seq.take(12))
                    }
                }
            }
        }
    }
})