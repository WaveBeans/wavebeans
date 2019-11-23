package mux.lib.execution

import mux.lib.AnyBean
import mux.lib.BeanStream

class SplittingPod(
        bean: BeanStream<*, *>,
        podKey: PodKey,
        partitionCount: Int
) : AbstractPod(podKey, bean, partitionCount) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}<SplitTo=${partitionCount}>"
}

