package io.wavebeans.execution.pod

import io.wavebeans.lib.*

abstract class SplittingPod<T : Any, B : BeanStream<T>>(
        bean: B,
        podKey: PodKey,
        partitionCount: Int,
        converter: (List<T>) -> TransferContainer,
        partitionSize: Int = DEFAULT_PARTITION_SIZE
) : AbstractPod<T, B>(podKey, bean, partitionCount, converter, partitionSize = partitionSize) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}<SplitTo=${partitionCount}>"
}

