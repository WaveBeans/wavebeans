package io.wavebeans.execution.pod

import io.wavebeans.lib.BeanStream

class AnyStreamingPod(
        bean: BeanStream<Any>,
        podKey: PodKey
) : StreamingPod<Any, BeanStream<Any>>(
        bean = bean,
        podKey = podKey
)