package io.wavebeans.execution.pod

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.stream.FiniteStream

class AnyStreamingPod(
        bean: BeanStream<Any>,
        podKey: PodKey
) : StreamingPod<Any, BeanStream<Any>>(
        bean = bean,
        podKey = podKey
)
class AnyFiniteStreamingPod(
        bean: FiniteStream<Any>,
        podKey: PodKey
) : StreamingPod<Any, FiniteStream<Any>>(
        bean = bean,
        podKey = podKey
)