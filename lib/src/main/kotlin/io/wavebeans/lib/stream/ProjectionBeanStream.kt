package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.WindowStream
import io.wavebeans.lib.stream.window.WindowStreamParams
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class ProjectionBeanStreamParams(
        val start: Long,
        val end: Long?,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

/**
 * Allows to read only specific time projection of the [BeanStream] with type of [Sample].
 */
class SampleProjectionBeanStream(
        override val input: BeanStream<Sample>,
        override val parameters: ProjectionBeanStreamParams
) : BeanStream<Sample>, SingleBean<Sample> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val start = timeToSampleIndexFloor(parameters.start, parameters.timeUnit, sampleRate)
                .let { if (it < 0) 0 else it }
                .toInt()
        val end = parameters.end
                ?.let { timeToSampleIndexCeil(it, parameters.timeUnit, sampleRate) }
                ?.toInt()
                ?: Int.MAX_VALUE
        val length = end - start
        return input.asSequence(sampleRate).drop(start).take(length) // TODO add support for Long?
    }

    override fun inputs(): List<AnyBean> = listOf(input)
}

class WindowSampleProjectionBeanStreamParams(
        windowSize: Int,
        step: Int,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : WindowStreamParams(windowSize, step) {
    init {
        require(step >= 1) { "Step should be more or equal to 1" }
        require(windowSize > 1) { "Window size should be more than 1" }
    }
}

/**
 * Allows to read only specific time projection of the [WindowStream] with type of [Sample].
 */
class WindowSampleProjectionBeanStream(
        override val input: WindowStream<Sample>,
        override val parameters: WindowSampleProjectionBeanStreamParams
) : WindowStream<Sample>, SingleBean<Window<Sample>> {

    override fun asSequence(sampleRate: Float): Sequence<Window<Sample>> {
        val startIdx = timeToSampleIndexFloor(parameters.start, parameters.timeUnit, sampleRate).toInt() / parameters.step
        val endIdx = parameters.end?.let { timeToSampleIndexCeil(it, parameters.timeUnit, sampleRate).toInt() }
                ?.let { it / parameters.step + if (it % parameters.step == 0) 0 else 1 }
                ?: Int.MAX_VALUE

        return input.asSequence(sampleRate)
                .drop(startIdx)
                .take(endIdx - startIdx)
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
fun BeanStream<Sample>.rangeProjection(start: Long, end: Long? = null, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): BeanStream<Sample> {
    return SampleProjectionBeanStream(this, ProjectionBeanStreamParams(start, end, timeUnit))
}

/**
 * Gets a projection of the object in the specified time range for [WindowStream]
 *
 * @param start starting point of the projection in time units.
 * @param end ending point of the projection (including) in time units. Null if do not limit
 * @param timeUnit the units the projection is defined in (i.e seconds, milliseconds, microseconds). TODO: replace TimeUnit with non-java.util.concurrent one
 *
 * @return the projection of specific time interval
 */
fun WindowStream<Sample>.rangeProjection(start: Long, end: Long? = null, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): WindowStream<Sample> {
    return WindowSampleProjectionBeanStream(this,
            WindowSampleProjectionBeanStreamParams(
                    this.parameters.windowSize, this.parameters.step, start, end, timeUnit
            )
    )
}