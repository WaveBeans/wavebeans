package io.wavebeans.lib.io

import io.wavebeans.lib.*
import mu.KotlinLogging

fun BeanStream<Sample>.toDevNull(): StreamOutput<Sample> = DevNullSampleStreamOutput(this)

class DevNullSampleStreamOutput(
        override val input: BeanStream<Sample>,
        override val parameters: NoParams = NoParams()
) : StreamOutput<Sample>, SinglePartitionBean {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun writer(sampleRate: Float): Writer {

        val sampleIterator = input.asSequence(sampleRate).iterator()
        var sampleCounter = 0L
        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    sampleIterator.next()
                    sampleCounter++
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
