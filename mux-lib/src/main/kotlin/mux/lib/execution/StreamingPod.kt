package mux.lib.execution

import mux.lib.AnyBean
import mux.lib.BeanStream

class StreamingPod(
        bean: BeanStream<*, *>,
        podKey: PodKey
) : AbstractPod(podKey, bean, 1) {

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}