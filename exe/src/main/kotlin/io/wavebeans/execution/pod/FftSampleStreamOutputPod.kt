package io.wavebeans.execution.pod

import io.wavebeans.lib.Bean
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.stream.fft.FftSample

class FftSampleStreamOutputPod(
        override val bean: StreamOutput<FftSample>,
        override val podKey: PodKey
) : AbstractStreamOutputPod<FftSample>() {

    override val input: Bean<FftSample>
        get() = bean
}