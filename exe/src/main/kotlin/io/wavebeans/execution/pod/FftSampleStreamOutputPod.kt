package io.wavebeans.execution.pod

import io.wavebeans.lib.Bean
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FiniteFftStream

class FftSampleStreamOutputPod(
        override val bean: StreamOutput<FftSample>,
        override val podKey: PodKey,
        override val sampleRate: Float
) : AbstractStreamOutputPod<FftSample>() {

    override val input: Bean<FftSample>
        get() = bean
}