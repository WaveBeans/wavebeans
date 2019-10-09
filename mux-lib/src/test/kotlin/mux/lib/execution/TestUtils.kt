package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.Sample
import mux.lib.sampleOf
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit

fun newTestPod(seq: List<Int>): StreamingPod<Sample, SampleStream> {
    return object : StreamingPod<Sample, SampleStream>(1, unclaimedElementsCleanupThreshold = 0) {
        override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException()

        override val parameters: BeanParams
            get() = throw UnsupportedOperationException()

        override fun asSequence(sampleRate: Float): Sequence<Sample> = seq.asSequence().map { sampleOf(it) }

        override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream = throw UnsupportedOperationException()

    }
}

