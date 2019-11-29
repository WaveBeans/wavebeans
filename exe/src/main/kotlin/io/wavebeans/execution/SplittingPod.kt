package io.wavebeans.execution

import io.wavebeans.lib.*

abstract class SplittingPod<T : Any, ARRAY_T : Any>(
        bean: BeanStream<T, *>,
        podKey: PodKey,
        partitionCount: Int,
        converter: (List<T>) -> ARRAY_T,
        partitionSize: Int = DEFAULT_PARTITION_SIZE
) : AbstractPod<T, ARRAY_T>(podKey, bean, partitionCount, converter, partitionSize = partitionSize) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}<SplitTo=${partitionCount}>"
}

class SampleSplittingPod(
        bean: BeanStream<Sample, *>,
        podKey: PodKey,
        partitionCount: Int
) : SplittingPod<Sample, SampleArray>(
        bean = bean,
        podKey = podKey,
        partitionCount = partitionCount,
        converter = { list ->
            val i = list.iterator()
            createSampleArray(list.size) { i.next() }
        }
)