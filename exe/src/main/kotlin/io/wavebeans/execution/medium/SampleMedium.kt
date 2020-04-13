package io.wavebeans.execution.medium

import io.wavebeans.lib.Sample

/**
 * [Medium] for [Sample]s
 */
class SampleMedium(
        val samples: DoubleArray
) : Medium {

    companion object {
        /**
         * Creates [SampleMedium] out of the list of samples
         *
         * @param list list of the samples to create
         */
        fun create(list: List<Sample>): SampleMedium {
            val i = list.iterator()
            val size: Int = list.size
            require(size >= 1) { "Can't create sample array of the size $size" }
            return SampleMedium(DoubleArray(size) { i.next() })
        }
    }

    override fun serializer(): MediumSerializer {
        TODO("Not yet implemented")
    }

    override fun extractElement(at: Int): Any? = if (at < samples.size) samples[at] else null
}
