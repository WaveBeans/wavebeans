package io.wavebeans.execution.pod

import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.Writer

abstract class AbstractStreamOutputPod<T: Any> : StreamOutput<T>, TickPod {

    abstract val bean: StreamOutput<T>

    // TODO that should be the part of configuration
    private val sampleRate = 44100.0f

    @Volatile
    private var isFinished = false

    private var writer: Writer? = null

    override fun start() {
        writer = bean.writer(sampleRate)
    }

    override fun tick(): Boolean {
        check(writer != null) { "Pod should be started first" }
        return if (!isFinished) {
            val isSomethingLeft = writer!!.write()
            if (!isSomethingLeft) isFinished = true
            true
        } else {
            false
        }
    }

    override fun close() {
        isFinished = true
        writer?.close()
    }

    override fun isFinished(): Boolean = isFinished

    override fun inputs(): List<AnyBean> = listOf(bean)

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long = throw UnsupportedOperationException("You can't read from this pod")

    override fun iteratorNext(iteratorKey: Long, buckets: Int): List<Sample>? = throw UnsupportedOperationException("You can't read from this pod")

    override fun writer(sampleRate: Float): Writer = throw UnsupportedOperationException("Not required by pod")

    override val parameters: BeanParams
        get() = NoParams()

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}