package mux.lib.execution

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import mux.lib.*
import mux.lib.stream.SampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object StreamingPodSpec : Spek({
    describe("StreamingPod returning predefined sequence") {

        val seq = (1..100).toList()
        val newPod: () -> StreamingPod<Sample, SampleStream> = {
            object : StreamingPod<Sample, SampleStream>(1) {
                override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException()

                override val parameters: BeanParams
                    get() = throw UnsupportedOperationException()

                override fun asSequence(sampleRate: Float): Sequence<Sample> = seq.asSequence().map { sampleOf(it) }

                override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream = throw UnsupportedOperationException()

            }
        }

        describe("Iterate over pod sequence") {
            val pod = newPod()

            val iteratorKey = pod.iteratorStart(0.0f)
            val result = (1..100)
                    .map { pod.iteratorNext(iteratorKey)!! }
                    .map { it.asInt() }

            it("should be the same as defined sequence") { assertThat(result).isEqualTo(seq) }

        }

        describe("Iterate over pod sequence with 2 proxies") {
            val pod = newPod()

            val iteratorKey1 = pod.iteratorStart(0.0f)
            val iteratorKey2 = pod.iteratorStart(0.0f)
            val result1 = (1..100)
                    .map { pod.iteratorNext(iteratorKey1)!! }
                    .map { it.asInt() }
            val result2 = (1..100)
                    .map { pod.iteratorNext(iteratorKey2)!! }
                    .map { it.asInt() }

            it("first should be the same as defined sequence") { assertThat(result1).isEqualTo(seq) }
            it("second should be the same as defined sequence") { assertThat(result2).isEqualTo(seq) }

        }

        describe("Iterate over pod sequence for more than defined in sequence") {
            val pod = newPod()

            val iteratorKey1 = pod.iteratorStart(0.0f)
            val e =
                    (1..101)
                            .map { pod.iteratorNext(iteratorKey1) }
                            .map { it?.asInt() }

            it("should be the same as defined sequence + [null]") {
                assertThat(e).isEqualTo(seq.plus(null as Int?))
            }

        }

        describe("Iterate over pod sequence with 2 proxies adding second in the middle") {
            val pod = newPod()

            val iteratorKey1 = pod.iteratorStart(0.0f)
            val result1 = (1..50)
                    .map { pod.iteratorNext(iteratorKey1)!! }
                    .map { it.asInt() }

            val iteratorKey2 = pod.iteratorStart(0.0f)
            val result2 = (51..100)
                    .map { pod.iteratorNext(iteratorKey2)!! }
                    .map { it.asInt() }

            val result11 = (51..100)
                    .map { pod.iteratorNext(iteratorKey1)!! }
                    .map { it.asInt() }

            it("first part of first iterator plus second part of first iterator should be the same as defined sequence") {
                assertThat(result1 + result11).isEqualTo(seq)
            }
            it("second should be second half of defined sequence") { assertThat(result2).isEqualTo(seq.drop(50)) }

        }

    }
})