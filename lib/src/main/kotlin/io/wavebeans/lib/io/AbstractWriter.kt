package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.metrics.outputClass
import io.wavebeans.metrics.samplesProcessedOnOutputMetric
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

abstract class AbstractWriter<T : Any>(
        val stream: BeanStream<T>,
        val sampleRate: Float,
        val writerDelegate: WriterDelegate,
        outputClazz: KClass<*>
) : Writer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        writerDelegate.headerFn = ::header
        writerDelegate.footerFn = ::footer
    }

    private val samplesCounter = samplesProcessedOnOutputMetric.withTags(outputClass to outputClazz.jvmName)

    protected abstract fun header(): ByteArray?

    protected abstract fun footer(): ByteArray?

    private val sampleIterator = stream.asSequence(sampleRate).iterator()

    override fun write(): Boolean {
        return if (sampleIterator.hasNext()) {
            val bytes = serialize(sampleIterator.next())
            writerDelegate.write(bytes, 0, bytes.size)
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
