package io.wavebeans.execution.pod

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PlainMedium
import io.wavebeans.execution.newTestSplittingPod
import io.wavebeans.lib.Sample
import io.wavebeans.lib.asInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe

object SplittingPodSpec : Spek({
    val seq = (1..100).toList()

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

            describe("SplittingPod returning predefined sequence. Two partitions") {

                describe("Iterate over pod sequence") {
                    val pod by memoized { newTestSplittingPod(seq, 2) }
                    val iteratorKeys by memoized { arrayOf(pod.iteratorStart(100.0f, 0), pod.iteratorStart(100.0f, 1)) }

                    it("Partition 0: should be the same as defined sequence even elements") {
                        assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[0], 50))?.map { it.asInt() })
                                .isEqualTo(seq.windowed(2, 2).map { it[0] })
                    }

                    it("Partition 1: should be the same as defined sequence odd elements") {
                        assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[1], 50))?.map { it.asInt() })
                                .isEqualTo(seq.windowed(2, 2).map { it[1] })
                    }

                }
            }

            describe("SplittingPod returning predefined sequence. One partition") {
                describe("Iterate over pod sequence") {
                    val pod by memoized { newTestSplittingPod(seq, 1) }

                    val iteratorKey0 by memoized { pod.iteratorStart(100.0f, 0) }

                    it("Partition 0: should be the same as defined sequence") {
                        assertThat(sampleRemap(pod.iteratorNext(iteratorKey0, 100))?.map { it.asInt() })
                                .isEqualTo(seq)
                    }
                }
            }

            describe("SplittingPod returning predefined sequence. Three partitions") {
                describe("Iterate over pod sequence") {
                    val pod by memoized(mode = CachingMode.SCOPE) { newTestSplittingPod(seq, 3) }

                    val iteratorKeys by memoized(mode = CachingMode.SCOPE) {
                        arrayOf(
                                pod.iteratorStart(100.0f, 0),
                                pod.iteratorStart(100.0f, 1),
                                pod.iteratorStart(100.0f, 2)
                        )
                    }

                    it("Partition 0: should be the same as defined sequence 0th elements in triplets") {
                        assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[0], 34))?.map { it.asInt() })
                                .isEqualTo(seq.windowed(3, 3, true).map { it[0] })
                    }

                    it("Partition 1: should be the same as defined sequence 0th elements in triplets") {
                        assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[1], 33))?.map { it.asInt() })
                                .isEqualTo(seq.windowed(3, 3, true).filter { it.size > 1 }.map { it[1] })
                    }

                    it("Partition 2: should be the same as defined sequence 0th elements in triplets") {
                        assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[2], 33))?.map { it.asInt() })
                                .isEqualTo(seq.windowed(3, 3, true).filter { it.size > 2 }.map { it[2] })
                    }
                }
            }

            describe("Iterate over pod sequence with 2 proxies. Two partitions") {
                val pod by memoized(mode = CachingMode.SCOPE) { newTestSplittingPod(seq, 2) }

                val iteratorKeys by memoized(mode = CachingMode.SCOPE) {
                    arrayOf(
                            pod.iteratorStart(100.0f, 0), pod.iteratorStart(100.0f, 0),
                            pod.iteratorStart(100.0f, 1), pod.iteratorStart(100.0f, 1)
                    )
                }

                it("Partition 0: first should be the same as defined sequence even elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[0], 50))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }
                it("Partition 0: second should be the same as defined sequence even elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[1], 50))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }

                it("Partition 1: first should be the same as defined sequence odd elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[2], 50))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
                it("Partition 1: second should be the same as defined sequence odd elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[3], 50))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
            }

            describe("Iterate over pod sequence for more than defined in sequence at once. Two partitions") {
                val pod by memoized(mode = CachingMode.SCOPE) { newTestSplittingPod(seq, 2) }

                val iteratorKeys by memoized(mode = CachingMode.SCOPE) {
                    arrayOf(pod.iteratorStart(100.0f, 0), pod.iteratorStart(100.0f, 1))
                }

                it("Partition 0: should be the same as defined sequence odd elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[0], 51))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }

                it("Partition 1: should be the same as defined sequence even elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[1], 51))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
            }

            describe("Iterate over pod sequence for more than defined in sequence in two attempts. Two partitions") {
                val pod by memoized(mode = CachingMode.SCOPE) { newTestSplittingPod(seq, 2) }

                val iteratorKeys by memoized(mode = CachingMode.SCOPE) {
                    arrayOf(pod.iteratorStart(100.0f, 0), pod.iteratorStart(100.0f, 1))
                }

                it("Partition 0: first attempt should be the same as defined sequence even elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[0], 50))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }
                it("Partition 0: second attempt should be null") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[0], 1))?.map { it.asInt() })
                            .isNull()
                }
                it("Partition 1: first attempt should be the same as defined sequence odd elements") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[1], 50))?.map { it.asInt() })
                            .isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
                it("Partition 1: second attempt should be null") {
                    assertThat(sampleRemap(pod.iteratorNext(iteratorKeys[1], 1))?.map { it.asInt() })
                            .isNull()
                }
            }
        }
    }
})