package io.wavebeans.execution.pod

import io.wavebeans.lib.Bean
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FiniteFftStream

class FftSampleStreamOutputPod(
        override val bean: StreamOutput<FftSample, FiniteFftStream>,
        override val podKey: PodKey
) : AbstractStreamOutputPod<FftSample, FiniteFftStream>() {

    override val input: Bean<FftSample, FiniteFftStream>
        get() = bean
}