package io.wavebeans.execution

import io.wavebeans.lib.*

abstract class StreamingPod<T: Any, ARRAY_T:Any>(
        bean: BeanStream<T, *>,
        podKey: PodKey,
        converter: (List<T>) -> ARRAY_T,
        partitionSize: Int = DEFAULT_PARTITION_SIZE
) : AbstractPod<T, ARRAY_T>(podKey, bean, 1, converter, partitionSize) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}

class SampleStreamingPod(
        bean: BeanStream<Sample, *>,
        podKey: PodKey
) : StreamingPod<Sample, SampleArray>(
        bean = bean,
        podKey = podKey,
        converter = { list ->
            val i = list.iterator()
            createSampleArray(list.size) { i.next() }
        }
)