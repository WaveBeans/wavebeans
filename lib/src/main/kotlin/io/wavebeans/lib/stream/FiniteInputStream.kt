package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

fun <T:Any> FiniteInput<T>.finiteSampleStream(): FiniteStream<T> = FiniteInputStream(this, NoParams())

fun <T: Any> FiniteInput<T>.stream(converter: FiniteToStream<T>): BeanStream<T> = this.finiteSampleStream().stream(converter)

class FiniteInputStream<T :Any>(
        override val input: FiniteInput<T>,
        override val parameters: NoParams
) : FiniteStream<T>, SingleBean<T> {

    override fun asSequence(sampleRate: Float): Sequence<T> = input.asSequence(sampleRate)

    override fun length(timeUnit: TimeUnit): Long = input.length(timeUnit)

}

