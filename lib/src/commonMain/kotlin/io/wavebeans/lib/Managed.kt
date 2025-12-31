package io.wavebeans.lib

import io.wavebeans.lib.stream.Measured
import io.wavebeans.lib.stream.SampleCountMeasurement
import kotlinx.serialization.Serializable

/**
 * The sample wrapper that allows to send the specific signals over the stream.
 */
@Serializable
data class Managed<S, A, T>(
        val signal: S,
        val argument: A?,
        val payload: T
) : Measured {
    override fun measure(): Int = SampleCountMeasurement.samplesInObject(payload as Any)
}