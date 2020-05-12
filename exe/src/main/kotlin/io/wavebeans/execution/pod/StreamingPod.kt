package io.wavebeans.execution.pod

import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.lib.*

abstract class StreamingPod<T : Any, B : BeanStream<T>>(
        bean: B,
        podKey: PodKey,
        partitionSize: Int = ExecutionConfig.partitionSize
) : AbstractPod<T, B>(podKey, bean, 1, partitionSize) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}

