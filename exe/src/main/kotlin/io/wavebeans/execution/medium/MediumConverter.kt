package io.wavebeans.execution.medium

import io.wavebeans.execution.pod.TransferContainer
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.window.Window

@Suppress("UNCHECKED_CAST")
object MediumConverter {

    fun convert(o: List<Any>): TransferContainer {
        return when (o.firstOrNull()) {
            is Sample -> {
                val list = o as List<Sample>
                val i = list.iterator()
                createSampleArray(list.size) { i.next() }
            }
            else -> throw UnsupportedOperationException("${o::class} is not supported")
        }
    }

    fun <T> convert(result: PodCallResult): T {
        return when (result.type) {
            "List<SampleArray>" -> result.nullableSampleArrayList() as T
            "List<WindowSampleArray>" -> result.nullableWindowSampleArrayList() as T
            else -> throw UnsupportedOperationException("${result.type} is not supported")
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun <T> extractElement(container: TransferContainer, at: Int): T {
        return when (container) {
            is SampleArray -> if (at < container.size) container[at] else null
            is Array<*> -> {
                when (val el = container.firstOrNull()) {
                    el == null -> null
                    is SampleArray -> if (at < container.size) Window((container[at] as SampleArray).toList()) else null
                    else -> throw UnsupportedOperationException("${el!!::class} is not supported")

                }
            }
            else -> throw UnsupportedOperationException("${container::class} is not supported")
        } as T
    }
}