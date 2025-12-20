package io.wavebeans.lib.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import io.wavebeans.lib.*
import kotlinx.serialization.Serializable

fun <T : Any, R : Any> BeanStream<T>.map(transform: (T) -> R): BeanStream<R> =
    MapStream(this, MapStreamParams(transform))

@Deprecated(
    message = "Use map(transform: (T) -> R) instead. This will be removed once all components are migrated off Fn.",
    replaceWith = ReplaceWith("this.map { transform.apply(it) }")
)
fun <T : Any, R : Any> BeanStream<T>.map(transform: Fn<T, R>): BeanStream<R> =
    this.map { transform.apply(it) }

@Serializable
data class MapStreamParams<T : Any, R : Any>(val transform: (T) -> R) : BeanParams

class MapStream<T : Any, R : Any>(
    override val input: BeanStream<T>,
    override val parameters: MapStreamParams<T, R>
) : AbstractOperationBeanStream<T, R>(input), AlterBean<T, R> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun operationSequence(input: Sequence<T>, sampleRate: Float): Sequence<R> {
        log.trace { "[$this] Initiating sequence Map(input = $input,parameters = $parameters)" }
        return input.map { parameters.transform.invoke(it) }
    }

}