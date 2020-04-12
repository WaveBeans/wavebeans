package io.wavebeans.execution.medium

import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window

typealias Medium = Any

@Suppress("UNCHECKED_CAST")
object MediumConverter {

    fun listToMedium(objects: List<Any>): Medium {
        return when (val e1 = objects.firstOrNull()) {
            is Sample -> {
                val list = objects as List<Sample>
                val i = list.iterator()
                createSampleArray(list.size) { i.next() }
            }
            is FftSample -> {
                val list = objects as List<FftSample>
                val i = list.iterator()
                createFftSampleArray(list.size) { i.next() }
            }
            is Window<*> -> {
                when (val e2 = e1.elements.firstOrNull()) {
                    is Sample -> {
                        val list = objects as List<Window<Sample>>
                        val i = list.iterator()
                        createWindowSampleArray(list.size, e1.size, e1.step) { i.next() }
                    }
                    else -> throw UnsupportedOperationException("${e2!!::class} is not supported")
                }
            }
            is List<*> -> {
                when (val e2 = e1.firstOrNull()) {
                    is Int -> (objects as List<List<Int>>).map { it.toIntArray() }.toTypedArray()
                    is Long -> (objects as List<List<Long>>).map { it.toLongArray() }.toTypedArray()
                    is Float -> (objects as List<List<Float>>).map { it.toFloatArray() }.toTypedArray()
                    is Double -> (objects as List<List<Double>>).map { it.toDoubleArray() }.toTypedArray()
                    else -> throw UnsupportedOperationException("${e2!!::class} is not supported")
                }
            }
            else -> throw UnsupportedOperationException("${e1!!::class} is not supported")
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun extractElement(medium: Medium, at: Int): Any? {
        return when (medium) {
            is SampleArray -> if (at < medium.size) medium[at] else null
            is WindowSampleArray -> {
                val size = medium.windowSize
                val step = medium.windowStep
                val windowSampleArray = medium.sampleArray
                if (at < windowSampleArray.size)
                    Window.ofSamples(size, step, windowSampleArray[at].toList())
                else
                    null
            }
            is Array<*> -> {
                when (val el = medium.firstOrNull()) {
                    el == null -> null
                    is FftSample -> {
                        val list = medium as Array<FftSample>
                        if (at < list.size) list[at] else null
                    }
                    is DoubleArray -> {
                        val list = medium as Array<DoubleArray>
                        if (at < list.size) list[at].toList() else null
                    }
                    else -> throw UnsupportedOperationException("${el!!::class} is not supported")
                }
            }
            else -> throw UnsupportedOperationException("${medium::class} is not supported")
        }
    }
}