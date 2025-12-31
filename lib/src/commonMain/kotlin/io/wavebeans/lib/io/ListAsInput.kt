package io.wavebeans.lib.io

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.SourceBean
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.stream.FiniteStream

fun <T : Any> List<T>.input(): FiniteStream<T> {
    require(this.isNotEmpty()) { "Input list should not be empty" }
    return ListAsInput(ListAsInputParams(this))
}

class ListAsInputParams(
    val list: List<Any>
) : BeanParams {
    override fun toString(): String {
        return "ListAsInputParams(list=$list)"
    }
}

class ListAsInput<T : Any>(
    override val parameters: ListAsInputParams
) : FiniteStream<T>, SourceBean<T> {

//    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to ListAsInput::class.jvmName)

    @Suppress("UNCHECKED_CAST")
    override fun asSequence(sampleRate: Float): Sequence<T> =
        parameters.list.asSequence()
            .map {
//            samplesProcessed.increment()
                it as T
            }

    override fun length(timeUnit: TimeUnit): Long = 0

    override val desiredSampleRate: Float? = null

    override fun samplesCount(): Long = parameters.list.size.toLong()
}