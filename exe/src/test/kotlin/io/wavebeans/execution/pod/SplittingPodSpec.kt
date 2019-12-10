package io.wavebeans.execution.pod

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.newTestSplittingPod
import io.wavebeans.lib.asInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SplittingPodSpec : Spek({
    val seq = (1..100).toList()
    describe("SplittingPod returning predefined sequence. Two partitions") {

        describe("Iterate over pod sequence") {
            newTestSplittingPod(seq, 2).use { pod ->
                val iteratorKey0 = pod.iteratorStart(100.0f, 0)
                val iteratorKey1 = pod.iteratorStart(100.0f, 1)

                describe("Partition 0") {
                    val result = pod.iteratorNext(iteratorKey0, 50)
                            ?.map {it as SampleArray }
                            ?.flatMap { it.asList() }
                            ?.map { it.asInt() }

                    it("should be the same as defined sequence even elements") { assertThat(result).isEqualTo(seq.windowed(2, 2).map { it[0] }) }
                }

                describe("Partition 1") {
                    val result = pod.iteratorNext(iteratorKey1, 50)
                            ?.map {it as SampleArray }
                            ?.flatMap { it.asList() }
                            ?.map { it.asInt() }

                    it("should be the same as defined sequence odd elements") { assertThat(result).isEqualTo(seq.windowed(2, 2).map { it[1] }) }
                }
            }

        }
    }

    describe("SplittingPod returning predefined sequence. One partition") {
        describe("Iterate over pod sequence") {
            newTestSplittingPod(seq, 1).use { pod ->

                val iteratorKey0 = pod.iteratorStart(100.0f, 0)

                describe("Partition 0") {
                    val result = pod.iteratorNext(iteratorKey0, 100)
                            ?.map {it as SampleArray }
                            ?.flatMap { it.asList() }
                            ?.map { it.asInt() }

                    it("should be the same as defined sequence") { assertThat(result).isEqualTo(seq) }
                }
            }

        }
    }

    describe("SplittingPod returning predefined sequence. Three partitions") {
        describe("Iterate over pod sequence") {
            newTestSplittingPod(seq, 3).use { pod ->

                val iteratorKey0 = pod.iteratorStart(100.0f, 0)
                val iteratorKey1 = pod.iteratorStart(100.0f, 1)
                val iteratorKey2 = pod.iteratorStart(100.0f, 2)

                describe("Partition 0") {
                    val result = pod.iteratorNext(iteratorKey0, 34)
                            ?.map {it as SampleArray }
                            ?.flatMap { it.asList() }
                            ?.map { it.asInt() }

                    it("should be the same as defined sequence 0th elements in triplets") {
                        assertThat(result).isEqualTo(seq.windowed(3, 3, true).map { it[0] })
                    }
                }
                describe("Partition 1") {
                    val result = pod.iteratorNext(iteratorKey1, 33)
                            ?.map {it as SampleArray }
                            ?.flatMap { it.asList() }
                            ?.map { it.asInt() }

                    it("should be the same as defined sequence 0th elements in triplets") {
                        assertThat(result).isEqualTo(seq.windowed(3, 3, true).filter { it.size > 1 }.map { it[1] })
                    }
                }
                describe("Partition 2") {
                    val result = pod.iteratorNext(iteratorKey2, 33)
                            ?.map {it as SampleArray }
                            ?.flatMap { it.asList() }
                            ?.map { it.asInt() }

                    it("should be the same as defined sequence 0th elements in triplets") {
                        assertThat(result).isEqualTo(seq.windowed(3, 3, true).filter { it.size > 2 }.map { it[2] })
                    }
                }
            }

        }
    }

    describe("Iterate over pod sequence with 2 proxies. Two partitions") {
        newTestSplittingPod(seq, 2).use { pod ->

            val iteratorKey1p0 = pod.iteratorStart(100.0f, 0)
            val iteratorKey2p0 = pod.iteratorStart(100.0f, 0)
            val iteratorKey1p1 = pod.iteratorStart(100.0f, 1)
            val iteratorKey2p1 = pod.iteratorStart(100.0f, 1)

            describe("Partition 0") {
                val result1 = pod.iteratorNext(iteratorKey1p0, 50)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }
                val result2 = pod.iteratorNext(iteratorKey2p0, 50)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }

                it("first should be the same as defined sequence even elements") {
                    assertThat(result1).isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }
                it("second should be the same as defined sequence even elements") {
                    assertThat(result2).isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }
            }

            describe("Partition 1") {
                val result1 = pod.iteratorNext(iteratorKey1p1, 50)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }
                val result2 = pod.iteratorNext(iteratorKey2p1, 50)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }

                it("first should be the same as defined sequence odd elements") {
                    assertThat(result1).isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
                it("second should be the same as defined sequence odd elements") {
                    assertThat(result2).isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
            }
        }
    }

    describe("Iterate over pod sequence for more than defined in sequence at once. Two partitions") {
        newTestSplittingPod(seq, 2).use { pod ->

            val iteratorKey0 = pod.iteratorStart(100.0f, 0)
            val iteratorKey1 = pod.iteratorStart(100.0f, 1)
            describe("Partition 0") {
                val e = pod.iteratorNext(iteratorKey0, 51)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }

                it("should be the same as defined sequence odd elements") {
                    assertThat(e).isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }
            }

            describe("Partition 1") {
                val e = pod.iteratorNext(iteratorKey1, 51)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }

                it("should be the same as defined sequence even elements") {
                    assertThat(e).isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
            }
        }
    }

    describe("Iterate over pod sequence for more than defined in sequence in two attempts. Two partitions") {
        newTestSplittingPod(seq, 2).use { pod ->

            val iteratorKey0 = pod.iteratorStart(100.0f, 0)
            val iteratorKey1 = pod.iteratorStart(100.0f, 1)

            describe("Partition 0") {
                val e1 = pod.iteratorNext(iteratorKey0, 50)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }
                val e2 = pod.iteratorNext(iteratorKey0, 1)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }

                it("first attempt should be the same as defined sequence even elements") {
                    assertThat(e1).isEqualTo(seq.windowed(2, 2, true).map { it[0] })
                }
                it("second attempt should be null") {
                    assertThat(e2).isNull()
                }
            }

            describe("Partition 1") {
                val e1 = pod.iteratorNext(iteratorKey1, 50)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }
                val e2 = pod.iteratorNext(iteratorKey1, 1)
                        ?.map {it as SampleArray }
                        ?.flatMap { it.asList() }
                        ?.map { it.asInt() }

                it("first attempt should be the same as defined sequence odd elements") {
                    assertThat(e1).isEqualTo(seq.windowed(2, 2, true).map { it[1] })
                }
                it("second attempt should be null") {
                    assertThat(e2).isNull()
                }
            }
        }
    }
})