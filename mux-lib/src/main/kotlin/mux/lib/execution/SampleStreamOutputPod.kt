package mux.lib.execution

import mux.lib.*
import mux.lib.io.StreamOutput
import mux.lib.io.Writer
import mux.lib.stream.FiniteSampleStream

class SampleStreamOutputPod(
        val bean: StreamOutput<Sample, FiniteSampleStream>,
        override val podKey: PodKey
) : StreamOutput<Sample, FiniteSampleStream>, TickPod {

    override fun inputs(): List<AnyBean> = listOf(bean)

    // TODO that should be the part of configuration
    private val sampleRate = 44100.0f

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long  = throw UnsupportedOperationException("You can't read from this pod")

    override fun iteratorNext(iteratorKey: Long, buckets: Int): List<Sample>? = throw UnsupportedOperationException("You can't read from this pod")

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