package io.wavebeans.execution.pod

import io.wavebeans.lib.Bean
import io.wavebeans.lib.io.StreamOutput

class AnyStreamOutputPod(
        override val bean: StreamOutput<Any>,
        override val podKey: PodKey,
        override val sampleRate: Float
) : AbstractStreamOutputPod<Any>() {

    override val input: Bean<Any>
        get() = bean
}