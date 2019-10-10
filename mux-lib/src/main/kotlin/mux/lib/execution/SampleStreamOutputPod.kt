package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.NoParams
import mux.lib.Sample
import mux.lib.io.StreamOutput
import mux.lib.io.Writer
import mux.lib.stream.FiniteSampleStream

class SampleStreamOutputPod(
        val bean: StreamOutput<Sample, FiniteSampleStream>,
        override val podKey: PodKey
) : StreamOutput<Sample, FiniteSampleStream>, Pod<Sample, FiniteSampleStream>, TickPod {

    // TODO that should be the part of configuration
    private val sampleRate = 44100.0f

    private val writer by lazy { bean.writer(sampleRate) }

    override fun tick(): Boolean {
        return writer.write()
    }

    override fun terminate() {
        writer.close()
    }

    override fun writer(sampleRate: Float): Writer = throw UnsupportedOperationException("Not required by pod")

    override val parameters: BeanParams
        get() = NoParams()

    override val input: Bean<Sample, FiniteSampleStream>
        get() = bean

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}