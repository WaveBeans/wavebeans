package io.wavebeans.lib.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import io.wavebeans.lib.*

fun <T : Any, R : Any> BeanStream<T>.map(transform: ExecutionScope.(T) -> R): BeanStream<R> =
    MapStream(this, MapStreamParams(EmptyScope, transform))

fun <T : Any, R : Any> BeanStream<T>.map(scope: ExecutionScope, transform: ExecutionScope.(T) -> R): BeanStream<R> =
    MapStream(this, MapStreamParams(scope, transform))

data class MapStreamParams<T : Any, R : Any>(
    val scope: ExecutionScope,
    val transform: ExecutionScope.(T) -> R
) : BeanParams

class MapStream<T : Any, R : Any>(
    override val input: BeanStream<T>,
    override val parameters: MapStreamParams<T, R>
) : AbstractOperationBeanStream<T, R>(input), AlterBean<T, R> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun operationSequence(input: Sequence<T>, sampleRate: Float): Sequence<R> {
        log.trace { "[$this] Initiating sequence Map(input = $input,parameters = $parameters)" }
        return input.map { parameters.transform.invoke(parameters.scope, it) }
    }

}