package io.wavebeans.execution.pod

import io.wavebeans.lib.*

abstract class StreamingPod<T : Any, B : BeanStream<T>>(
        bean: B,
        podKey: PodKey,
        converter: (List<T>) -> TransferContainer,
        partitionSize: Int = DEFAULT_PARTITION_SIZE
) : AbstractPod<T, B>(podKey, bean, 1, converter, partitionSize) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}

