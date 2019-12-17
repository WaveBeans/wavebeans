package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.MediumConverter
import io.wavebeans.lib.BeanStream

class AnyStreamingPod(
        bean: BeanStream<Any>,
        podKey: PodKey
) : StreamingPod<Any, BeanStream<Any>>(
        bean = bean,
        podKey = podKey,
        converter = MediumConverter::convert
)