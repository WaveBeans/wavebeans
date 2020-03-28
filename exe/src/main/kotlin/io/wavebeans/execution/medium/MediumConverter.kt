package io.wavebeans.execution.medium

import io.wavebeans.execution.pod.TransferContainer
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window

@Suppress("UNCHECKED_CAST")
object MediumConverter {

    fun convert(o: List<Any>): TransferContainer {
        return when (val e1 = o.firstOrNull()) {
            is Sample -> {
                val list = o as List<Sample>
                val i = list.iterator()
                createSampleArray(list.size) { i.next() }
            }
            is FftSample -> {
                val list = o as List<FftSample>
                val i = list.iterator()
                createFftSampleArray(list.size) { i.next() }
            }
            is Window<*> -> {
                when (val e2 = e1.elements.firstOrNull()) {
                    is Sample -> {
                        val list = o as List<Window<Sample>>
                        val i = list.iterator()
                        createWindowSampleArray(list.size, e1.size, e1.step) { i.next() }
                    }
                    else -> throw UnsupportedOperationException("${e2!!::class} is not supported")
                }
            }
            is List<*> -> {
                when (val e2 = e1.firstOrNull()) {
                    is Int -> (o as List<List<Int>>).map { it.toIntArray() }.toTypedArray()
                    is Long -> (o as List<List<Long>>).map { it.toLongArray() }.toTypedArray()
                    is Float -> (o as List<List<Float>>).map { it.toFloatArray() }.toTypedArray()
                    is Double -> (o as List<List<Double>>).map { it.toDoubleArray() }.toTypedArray()
                    else -> throw UnsupportedOperationException("${e2!!::class} is not supported")
                }
            }
            else -> throw UnsupportedOperationException("${e1!!::class} is not supported")
        }
    }

    fun <T> convert(result: PodCallResult): T {
        return when (result.throwIfError().type) {
            "List<SampleArray>" -> result.nullableSampleArrayList() as T
            "List<WindowSampleArray>" -> result.nullableWindowSampleArrayList() as T
            "List<Array<FftSample>>" -> result.nullableFftSampleArrayList() as T
            "List<Array<DoubleArray>>" -> result.nullableDoubleArrayArrayList() as T
            else -> throw UnsupportedOperationException("${result.type} is not supported")
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun <T> extractElement(container: TransferContainer, at: Int): T {
        return when (container) {
            is SampleArray -> if (at < container.size) container[at] else null
            is WindowSampleArray -> {
                val size = container.windowSize
                val step = container.windowStep
                val windowSampleArray = container.sampleArray
                if (at < windowSampleArray.size)
                    Window.ofSamples(size, step, windowSampleArray[at].toList())
                else
                    null
            }
            is Array<*> -> {
                when (val el = container.firstOrNull()) {
                    el == null -> null
                    is FftSample -> {
                        val list = container as Array<FftSample>
                        if (at < list.size) list[at] else null
                    }
                    is DoubleArray -> {
                        val list = container as Array<DoubleArray>
                        if (at < list.size) list[at].toList() else null
                    }
                    else -> throw UnsupportedOperationException("${el!!::class} is not supported")
                }
            }
            else -> throw UnsupportedOperationException("${container::class} is not supported")
        } as T
    }
}