package io.wavebeans.execution

import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanStream

class SplittingPod(
        bean: BeanStream<*, *>,
        podKey: PodKey,
        partitionCount: Int
) : AbstractPod(podKey, bean, partitionCount) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}<SplitTo=${partitionCount}>"
}

