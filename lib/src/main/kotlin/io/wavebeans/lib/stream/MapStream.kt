package io.wavebeans.lib.stream

import io.wavebeans.lib.AlterBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream

fun <T : Any, R : Any> BeanStream<T>.map(transform: (T) -> R): BeanStream<R> = MapStream(this, AlterFnStreamParams(transform))

class AlterFnStreamParams<T : Any, R : Any>(val transform: (T) -> R) : BeanParams()

class MapStream<T : Any, R : Any>(
        override val input: BeanStream<T>,
        override val parameters: AlterFnStreamParams<T, R>
) : BeanStream<R>, AlterBean<T, R> {

    override fun asSequence(sampleRate: Float): Sequence<R> =
            input.asSequence(sampleRate).map { parameters.transform(it) }

}