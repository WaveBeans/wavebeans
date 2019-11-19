package mux.lib.execution

import assertk.assertThat
import assertk.assertions.*
import mux.lib.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object StreamingPodSpec : Spek({
    describe("StreamingPod returning predefined sequence") {
        val seq = (1..100).toList()

        describe("Iterate over pod sequence") {
            newTestStreamingPod(seq).use { pod ->

                val iteratorKey = pod.iteratorStart(100.0f, 0)
                val result = pod.iteratorNext(iteratorKey, 100)
                        ?.map { (it as Sample).asInt() }

                it("should be the same as defined sequence") { assertThat(result).isEqualTo(seq) }
            }

        }

        describe("Iterate over pod sequence with 2 proxies") {
            newTestStreamingPod(seq).use { pod ->

                val iteratorKey1 = pod.iteratorStart(100.0f, 0)
                val iteratorKey2 = pod.iteratorStart(100.0f, 0)
                val result1 = pod.iteratorNext(iteratorKey1, 100)
                        ?.map { (it as Sample).asInt() }
                val result2 = pod.iteratorNext(iteratorKey2, 100)
                        ?.map { (it as Sample).asInt() }

                it("first should be the same as defined sequence") { assertThat(result1).isEqualTo(seq) }
                it("second should be the same as defined sequence") { assertThat(result2).isEqualTo(seq) }
            }

        }

        describe("Iterate over pod sequence for more than defined in sequence at once") {
            newTestStreamingPod(seq).use { pod ->

                val iteratorKey1 = pod.iteratorStart(100.0f, 0)
                val e = pod.iteratorNext(iteratorKey1, 101)
                        ?.map { (it as Sample).asInt() }

                it("should be the same as defined sequence") {
                    assertThat(e).isEqualTo(seq)
                }
            }
        }

        describe("Iterate over pod sequence for more than defined in sequence in two attempts") {
            newTestStreamingPod(seq).use { pod ->

                val iteratorKey1 = pod.iteratorStart(100.0f, 0)
                val e1 = pod.iteratorNext(iteratorKey1, 100)
                        ?.map { (it as Sample).asInt() }
                val e2 = pod.iteratorNext(iteratorKey1, 1)
                        ?.map { (it as Sample).asInt() }

                it("first attempt should be the same as defined sequence") {
                    assertThat(e1).isEqualTo(seq)
                }
                it("second attempt should be null") {
                    assertThat(e2).isNull()
                }
            }
        }
    }
})