package io.wavebeans.execution.pod

import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.lib.*

abstract class SplittingPod<T : Any, B : BeanStream<T>>(
        bean: B,
        podKey: PodKey,
        partitionCount: Int,
        partitionSize: Int = ExecutionConfig.partitionSize
) : AbstractPod<T, B>(podKey, bean, partitionCount, partitionSize = partitionSize) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}<SplitTo=${partitionCount}>"
}

