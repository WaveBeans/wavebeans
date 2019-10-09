package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.BeanStream
import mux.lib.Sample
import java.util.concurrent.TimeUnit

abstract class StreamingPodProxy<S : Any>(
        override val pointedTo: PodKey,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val timeToReadAtOnce: Long = 500L,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanStream<Sample, S>, PodProxy<Sample, S> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)
        val iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate").long()

        return object : Iterator<Sample> {

            var samples: List<Sample>? = null
            var pointer = 0

            override fun hasNext(): Boolean {
                val s = tryReadSamples()
                return s != null && s.isNotEmpty() && pointer < s.size
            }

            override fun next(): Sample {
                val s = tryReadSamples()
                        ?: throw NoSuchElementException("Pod $pointedTo has no more samples")
                return s[pointer++]
            }

            private fun tryReadSamples(): List<Sample>? {
                if (samples == null || pointer >= samples?.size ?: 0) {
                    samples = caller.call(
                            "iteratorNext" +
                                    "?iteratorKey=$iteratorKey" +
                                    "&length=$timeToReadAtOnce" +
                                    "&timeUnit=$timeUnit"
                    ).nullableSampleList()
                    pointer = 0
                }
                return samples
            }
        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun toString(): String = "${this::class.simpleName}->[$pointedTo]"
}