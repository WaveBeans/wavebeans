package io.wavebeans.execution.pod

import io.wavebeans.lib.Bean
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.stream.FiniteSampleStream

class SampleStreamOutputPod(
        override val bean: StreamOutput<Sample>,
        override val podKey: PodKey,
        override val sampleRate: Float
) : AbstractStreamOutputPod<Sample>() {

    override val input: Bean<Sample>
        get() = bean
}