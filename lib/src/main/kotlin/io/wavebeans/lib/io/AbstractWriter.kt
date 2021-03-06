package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Managed
import io.wavebeans.metrics.*
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

    private val samplesCounter = samplesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val bytesCounter = bytesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val outputStateMetric = io.wavebeans.metrics.outputStateMetric.withTags(clazzTag to outputClazz.jvmName)
    private val gateStateMetric = gateStateOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)

    init {
        gateStateMetric.set(1.0)
        writerDelegate.initBuffer(null)
        outputStateMetric.set(1.0)
    }

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
        writerDelegate.finalizeBuffer(null, ::header, ::footer)
        writerDelegate.close(::header, ::footer)
        gateStateMetric.set(0.0)
        outputStateMetric.set(0.0)
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

    private val samplesProcessedMetric = samplesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val samplesSkippedMetric = samplesSkippedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val flushedCounterMetric = flushedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val outputStateMetric = io.wavebeans.metrics.outputStateMetric.withTags(clazzTag to outputClazz.jvmName)
    private val gateStateMetric = gateStateOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)
    private val bytesCounter = bytesProcessedOnOutputMetric.withTags(clazzTag to outputClazz.jvmName)

    @Volatile
    private var isGateOpened = true

    @Volatile
    private var isOutputOpened = true

    init {
        gateStateMetric.set(1.0)
        writerDelegate.initBuffer(null)
        outputStateMetric.set(1.0)
    }

    protected abstract fun header(): ByteArray?

    protected abstract fun footer(): ByteArray?

    private val sampleIterator = stream.asSequence(sampleRate).iterator()

    override fun write(): Boolean {

        return if (isOutputOpened && sampleIterator.hasNext()) {
            val next = sampleIterator.next()

            fun doWrite() {
                if (isOutputOpened) {
                    if (isGateOpened) {
                        val bytes = serialize(next.payload)
                        writerDelegate.write(bytes, 0, bytes.size)
                        samplesProcessedMetric.increment()
                        bytesCounter.increment(bytes.size.toDouble())
                    } else {
                        skip(next.payload)
                        samplesSkippedMetric.increment()
                    }
                }
            }

            when (next.signal) {
                FlushOutputSignal -> {
                    log.debug { "[$this] Got FlushOutputSignal [argument=${next.argument}]" }
                    writerDelegate.flush(next.argument, ::header, ::footer)
                    log.info { "[$this] The writer flushed [argument=${next.argument}]" }
                    flushedCounterMetric.increment()
                }
                OpenGateOutputSignal -> {
                    if (!isGateOpened) {
                        log.debug { "[$this] Got OpenGateOutputSignal [isGateOpened=$isGateOpened, argument=${next.argument}]" }
                        isGateOpened = true
                        writerDelegate.initBuffer(next.argument)
                        log.info { "[$this] The writer has opened the gate [argument=${next.argument}]" }
                        gateStateMetric.increment(1.0)
                    } // else signal is ignored
                }
                CloseGateOutputSignal -> {
                    if (isGateOpened) {
                        log.debug { "[$this] Got CloseGateOutputSignal [isGateOpened=$isGateOpened, argument=${next.argument}]" }
                        isGateOpened = false
                        writerDelegate.finalizeBuffer(next.argument, ::header, ::footer)
                        log.info { "[$this] The writer has closed the gate [argument=${next.argument}]" }
                        gateStateMetric.decrement(1.0)
                    } // else signal is ignored
                }
                CloseOutputSignal -> {
                    log.debug { "[$this] Got CloseOutputSignal [isGateOpened=$isGateOpened, argument=${next.argument}]" }
                    doWrite()
                    isOutputOpened = false
                    if (isGateOpened) {
                        writerDelegate.finalizeBuffer(next.argument, ::header, ::footer)
                        isGateOpened = false
                        gateStateMetric.set(0.0)
                    }
                    outputStateMetric.set(0.0)
                }
            }
            doWrite()
            true
        } else {
            log.debug { "[$this] The stream is over" }
            false
        }
    }

    protected abstract fun serialize(element: T): ByteArray

    protected abstract fun skip(element: T)

    override fun close() {
        writerDelegate.close(::header, ::footer)
        gateStateMetric.set(0.0)
        outputStateMetric.set(0.0)
    }
}

/**
 * The signals that [AbstractPartialWriter] handles.
 *
 * Known usages:
 *  * [NoopOutputSignal]
 *  * [FlushOutputSignal]
 *  * [OpenGateOutputSignal]
 *  * [CloseGateOutputSignal]
 *  * [CloseOutputSignal]
 */
typealias OutputSignal = Byte

/**
 * Perform no operation on the signal.
 */
const val NoopOutputSignal: OutputSignal = 0x00

/**
 * Flush the current buffer via [WriterDelegate.flush]. The argument to be by passed to that function.
 * Technically performs the same as [CloseGateOutputSignal] and [OpenGateOutputSignal] called on the same sample.
 */
const val FlushOutputSignal: OutputSignal = 0x01

/**
 * Controls the writing gate of the output. If it is opened the output performs the output to the specified output.
 * * When the output is created the gate is opened.
 * * The buffer is newly created in case the signal is caught or at the beginning of the stream.
 */
const val OpenGateOutputSignal: OutputSignal = 0x02

/**
 * Controls the writing gate of the output. If it is closed the output skips all provided samples.
 * * When the output is created the gate is opened.
 * * The current buffer is flushed when caught the signal. The sample provided with the singal is not skipped.
 */
const val CloseGateOutputSignal: OutputSignal = 0x03

/**
 * Controls if the output is closed. Even if the actual stream is not over, the signal may close the output.
 * Very convenient to stream infinite streams into a file.
 */
const val CloseOutputSignal: OutputSignal = 0x04

fun <T : Any, A : Any> T.withOutputSignal(signal: OutputSignal, argument: A? = null): Managed<OutputSignal, A, T> = Managed(signal, argument, this)

