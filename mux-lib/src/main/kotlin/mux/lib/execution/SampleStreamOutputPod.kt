package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.NoParams
import mux.lib.Sample
import mux.lib.io.StreamOutput
import mux.lib.io.Writer
import mux.lib.stream.FiniteSampleStream

class SampleStreamOutputPod(val bean: StreamOutput<Sample, FiniteSampleStream>) : StreamOutput<Sample, FiniteSampleStream>, Pod<Sample, FiniteSampleStream> {

    override fun writer(sampleRate: Float): Writer {
        return bean.writer(sampleRate)
    }

    override fun close() {
        bean.close()
    }

    override val parameters: BeanParams
        get() = NoParams()

    override val input: Bean<Sample, FiniteSampleStream>
        get() = bean
}