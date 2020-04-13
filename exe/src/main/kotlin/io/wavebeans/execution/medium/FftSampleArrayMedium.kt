package io.wavebeans.execution.medium

import io.wavebeans.lib.stream.fft.FftSample

/**
 * Type alias for array of [FftSample]
 */
class FftSampleArrayMedium(
        val samples: Array<FftSample>
) : Medium {

    companion object {
        /**
         * Creates [FftSampleArrayMedium] out of the list of [FftSample]s
         *
         * @param list to use to create a [Medium]
         */
        fun create(list: List<FftSample>): FftSampleArrayMedium {
            val size: Int = list.size
            require(size >= 1) { "Can't create sample array of the size $size" }
            val i = list.iterator()
            return FftSampleArrayMedium(Array(size) { i.next() })
        }
    }

    override fun serializer(): Serializer {
        TODO("Not yet implemented")
    }

    override fun extractElement(at: Int): Any? = if (at < samples.size) samples[at] else null
}

