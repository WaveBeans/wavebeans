package mux.lib.execution

import mux.lib.*
import java.util.concurrent.TimeUnit

abstract class StreamingPodProxy<S : Any>(
        override val pointedTo: PodKey,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val timeToReadAtOnce: Int = 10,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanStream<SampleArray, S>, PodProxy<SampleArray, S> {

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)
        val iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate").long()

        return object : Iterator<SampleArray> {

            var samples: List<SampleArray>? = null
            var pointer = 0

            override fun hasNext(): Boolean {
                val s = tryReadSamples()
                return s != null && s.isNotEmpty() && pointer < s.size
            }

            override fun next(): SampleArray {
                val s = tryReadSamples()
                        ?: throw NoSuchElementException("Pod $pointedTo has no more samples")
                return s[pointer++]
            }

            private fun tryReadSamples(): List<SampleArray>? {
                if (samples == null || pointer >= samples?.size ?: 0) {
                    samples = caller.call(
                            "iteratorNext" +
                                    "?iteratorKey=$iteratorKey" +
                                    "&buckets=$timeToReadAtOnce"
                    ).nullableSampleArrayList()
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