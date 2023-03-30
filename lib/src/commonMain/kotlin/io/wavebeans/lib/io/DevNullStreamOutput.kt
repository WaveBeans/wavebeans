package io.wavebeans.lib.io

import io.wavebeans.lib.*
import mu.KotlinLogging

fun <T : Any> BeanStream<T>.toDevNull(): StreamOutput<T> = DevNullStreamOutput(this)

class DevNullStreamOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: NoParams = NoParams()
) : AbstractStreamOutput<T>(input), SinglePartitionBean {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun outputWriter(inputSequence: Sequence<T>, sampleRate: Float): Writer {
//        val samplesProcessed = samplesProcessedOnOutputMetric.withTags(clazzTag to DevNullStreamOutput::class.jvmName)
        val sampleIterator = input.asSequence(sampleRate).iterator()
        var sampleCounter = 0L
        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    sampleIterator.next()
                    sampleCounter++
//                    samplesProcessed.increment()
                    true
                } else {
                    false
                }
            }

            override fun close() {
                log.debug { "[/DEV/NULL] Written $sampleCounter samples" }
            }
        }
    }
}
