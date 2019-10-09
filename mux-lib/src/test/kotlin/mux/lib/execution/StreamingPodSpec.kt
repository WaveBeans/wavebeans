package mux.lib.execution

import assertk.assertThat
import assertk.assertions.*
import mux.lib.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS

object StreamingPodSpec : Spek({
    describe("StreamingPod returning predefined sequence") {
        val seq = (1..100).toList()

        describe("Iterate over pod sequence") {
            val pod = newTestPod(seq)

            val iteratorKey = pod.iteratorStart(100.0f)
            val result = pod.iteratorNext(iteratorKey, 1000L, MILLISECONDS)
                    ?.map { it.asInt() }

            it("should be the same as defined sequence") { assertThat(result).isEqualTo(seq) }

        }

        describe("Iterate over pod sequence with 2 proxies") {
            val pod = newTestPod(seq)

            val iteratorKey1 = pod.iteratorStart(100.0f)
            val iteratorKey2 = pod.iteratorStart(100.0f)
            val result1 = pod.iteratorNext(iteratorKey1, 1000L, MILLISECONDS)
                    ?.map { it.asInt() }
            val result2 = pod.iteratorNext(iteratorKey2, 1000L, MILLISECONDS)
                    ?.map { it.asInt() }

            it("first should be the same as defined sequence") { assertThat(result1).isEqualTo(seq) }
            it("second should be the same as defined sequence") { assertThat(result2).isEqualTo(seq) }

        }

        describe("Iterate over pod sequence for more than defined in sequence at once") {
            val pod = newTestPod(seq)

            val iteratorKey1 = pod.iteratorStart(100.0f)
            val e = pod.iteratorNext(iteratorKey1, 1001L, MILLISECONDS)
                    ?.map { it.asInt() }

            it("should be the same as defined sequence + [null]") {
                assertThat(e).isEqualTo(seq)
            }

        }

        describe("Iterate over pod sequence for more than defined in sequence in two attempts") {
            val pod = newTestPod(seq)

            val iteratorKey1 = pod.iteratorStart(100.0f)
            val e1 = pod.iteratorNext(iteratorKey1, 1000L, MILLISECONDS)
                    ?.map { it.asInt() }
            val e2 = pod.iteratorNext(iteratorKey1, 1L, MILLISECONDS)
                    ?.map { it.asInt() }

            it("first attempt should be the same as defined sequence") {
                assertThat(e1).isEqualTo(seq)
            }
            it("second attempt should be null") {
                assertThat(e2).isNull()
            }

        }

        describe("Iterate over pod sequence with 2 proxies adding second in the middle") {
            val pod = newTestPod(seq)

            val iteratorKey1 = pod.iteratorStart(100.0f)
            val result1 = pod.iteratorNext(iteratorKey1, 500L, MILLISECONDS)
                    ?.map { it.asInt() }
                    ?: emptyList()

            val iteratorKey2 = pod.iteratorStart(100.0f)
            val result2 = pod.iteratorNext(iteratorKey2, 500L, MILLISECONDS)
                    ?.map { it.asInt() }

            val result11 = pod.iteratorNext(iteratorKey1, 500L, MILLISECONDS)
                    ?.map { it.asInt() }
                    ?: emptyList()

            it("first part of first iterator plus second part of first iterator should be the same as defined sequence") {
                assertThat(result1 + result11).isEqualTo(seq)
            }
            it("second should be second half of defined sequence") { assertThat(result2).isEqualTo(seq.drop(50)) }

        }

    }
})