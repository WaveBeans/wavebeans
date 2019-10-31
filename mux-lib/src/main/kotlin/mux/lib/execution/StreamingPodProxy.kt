package mux.lib.execution

import mux.lib.*
import java.util.concurrent.TimeUnit

abstract class StreamingPodProxy<T : Any, S : Any>(
        override val pointedTo: PodKey,
        override val partition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val timeToReadAtOnce: Int = 10,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        val converter: (PodCallResult) -> List<T>?
) : BeanStream<T, S>, PodProxy<T, S> {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)
        val iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate").long()

        return object : Iterator<T> {

            var samples: List<T>? = null
            var pointer = 0

            override fun hasNext(): Boolean {
                val s = tryReadSamples()
                return s != null && s.isNotEmpty() && pointer < s.size
            }

            override fun next(): T {
                val s = tryReadSamples()
                        ?: throw NoSuchElementException("Pod $pointedTo has no more samples")
                return s[pointer++]
            }

            private fun tryReadSamples(): List<T>? {
                if (samples == null || pointer >= samples?.size ?: 0) {
                    samples = converter(caller.call(
                            "iteratorNext" +
                                    "?iteratorKey=$iteratorKey" +
                                    "&buckets=$timeToReadAtOnce"
                    ))
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

    override fun toString(): String = "${this::class.simpleName}->[$pointedTo:$partition]"
}