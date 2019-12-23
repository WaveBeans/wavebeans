package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.WindowStream
import io.wavebeans.lib.stream.window.WindowStreamParams
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSupertypeOf

@Serializable
data class ProjectionBeanStreamParams(
        val start: Long,
        val end: Long?,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

/**
 * Allows to read only specific time projection of the [BeanStream] with type of [Sample].
 */
class ProjectionBeanStream<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: ProjectionBeanStreamParams
) : BeanStream<T>, SingleBean<T> {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val start = timeToSampleIndexFloor(parameters.start, parameters.timeUnit, sampleRate)
                .let { if (it < 0) 0 else it }
        val end = parameters.end
                ?.let { timeToSampleIndexCeil(it, parameters.timeUnit, sampleRate) }
                ?: Long.MAX_VALUE
        var leftToRead = end - start
        var toSkip = start
        val iterator = input.asSequence(sampleRate).iterator()
        while (toSkip > 0 && iterator.hasNext()) {
            val obj = iterator.next()
            toSkip -= SampleCountMeasurement.samplesInObject(obj)
        }
        leftToRead += toSkip // if we've over-read we may need to read less
        return object : Iterator<T> {
            override fun hasNext(): Boolean = leftToRead > 0 && iterator.hasNext()

            override fun next(): T {
                if (leftToRead > 0) {
                    val obj = iterator.next()
                    leftToRead -= SampleCountMeasurement.samplesInObject(obj)
                    return obj
                } else {
                    throw IllegalStateException("Has no more elements to read")
                }
            }

        }.asSequence()
    }

    override fun inputs(): List<AnyBean> = listOf(input)
}

/**
 * Gets a projection of the object in the specified time range for [BeanStream]
 *
 * @param start starting point of the projection in time units.
 * @param end ending point of the projection (including) in time units. Null if do not limit
 * @param timeUnit the units the projection is defined in (i.e seconds, milliseconds, microseconds). TODO: replace TimeUnit with non-java.util.concurrent one
 *
 * @return the projection of specific time interval
 */
fun <T : Any> BeanStream<T>.rangeProjection(start: Long, end: Long? = null, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): BeanStream<T> {
    return ProjectionBeanStream(this, ProjectionBeanStreamParams(start, end, timeUnit))
}

@Suppress("UNCHECKED_CAST")
object SampleCountMeasurement {

    private val types = mutableMapOf<KClass<*>, (Any) -> Int>()

    init {
        registerType(Sample::class) { 1 }
        registerType(Window::class) { window -> window.step * samplesInObject(window.elements.first()!!) }
    }

    fun <T : Any> registerType(clazz: KClass<T>, measurer: (T) -> Int) {
        check(types.put(clazz, measurer as (Any) -> Int) == null) { "$clazz is already registered" }
    }

    fun samplesInObject(obj: Any): Int {
        return types.filterKeys { obj::class.isSubclassOf(it) }
                .map { it.value }
                .first().invoke(obj)
    }
}