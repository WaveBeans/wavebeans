package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.stream.Measured
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.metrics.bytesProcessedOnOutputMetric
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.flushedOnOutputMetric
import io.wavebeans.metrics.samplesProcessedOnOutputMetric
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

abstract class AbstractWriter<T : Any>(
        val stream: BeanStream<T>,
        val sampleRate: Float,
        val writerDelegate: WriterDelegate<in Unit>,
        outputClazz: KClass<*>
) : Writer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        writerDelegate.headerFn = ::header
        writerDelegate.footerFn = ::footer
    }

    private val samplesCounter = samplesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val bytesCounter = bytesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)

    protected abstract fun header(): ByteArray?

    protected abstract fun footer(): ByteArray?

    private val sampleIterator = stream.asSequence(sampleRate).iterator()

    override fun write(): Boolean {
        return if (sampleIterator.hasNext()) {
            val bytes = serialize(sampleIterator.next())
            writerDelegate.write(bytes, 0, bytes.size)
            samplesCounter.increment()
            bytesCounter.increment(bytes.size.toDouble())
            true
        } else {
            log.debug { "[$this] The stream is over" }
            false
        }
    }

    protected abstract fun serialize(element: T): ByteArray

    override fun close() {
        writerDelegate.close()
    }

}

abstract class AbstractPartialWriter<T : Any, A : Any>(
        val stream: BeanStream<Managed<OutputSignal, A, T>>,
        val sampleRate: Float,
        val writerDelegate: WriterDelegate<A>,
        outputClazz: KClass<*>
) : Writer {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        writerDelegate.headerFn = ::header
        writerDelegate.footerFn = ::footer
    }

    private val samplesCounter = samplesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val flushedCounter = flushedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)

    protected abstract fun header(): ByteArray?

    protected abstract fun footer(): ByteArray?

    private val sampleIterator = stream.asSequence(sampleRate).iterator()

    override fun write(): Boolean {
        return if (sampleIterator.hasNext()) {
            val next = sampleIterator.next()
            if (next.signal == FlushOutputSignal) {
                log.debug { "[$this] The writer flushed" }
                writerDelegate.flush(next.argument)
                flushedCounter.increment()
            }
            val bytes = serialize(next.payload)
            writerDelegate.write(bytes, 0, bytes.size)
            samplesCounter.increment()
            true
        } else {
            log.debug { "[$this] The stream is over" }
            false
        }
    }

    protected abstract fun serialize(element: T): ByteArray

    override fun close() {
        writerDelegate.close()
    }
}

typealias OutputSignal = Byte

const val NoopOutputSignal: OutputSignal = 0x00
const val FlushOutputSignal: OutputSignal = 0x02

/**
 * The sample wrapper that allows to send the specific signals over the stream.
 */
@Serializable
data class Managed<S, A, T>(
        val signal: S,
        val argument: A?,
        val payload: T
) : Measured {
    override fun measure(): Int = SampleCountMeasurement.samplesInObject(payload as Any)
}

fun <T : Any, A : Any> T.withOutputSignal(signal: OutputSignal, argument: A? = null): Managed<OutputSignal, A, T> = Managed(signal, argument, this)

