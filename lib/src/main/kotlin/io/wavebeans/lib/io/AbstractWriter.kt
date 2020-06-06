package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

abstract class AbstractWriter<T : Any>(
        val stream: BeanStream<T>,
        val sampleRate: Float,
        val writerDelegate: WriterDelegate
) : Writer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        writerDelegate.headerFn = ::header
        writerDelegate.footerFn = ::footer
    }

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
