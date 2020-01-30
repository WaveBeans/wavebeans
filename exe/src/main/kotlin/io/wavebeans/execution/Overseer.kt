package io.wavebeans.execution

import io.wavebeans.lib.io.StreamOutput
import java.io.Closeable
import java.util.concurrent.Future

/**
 * Evaluates all outputs. The way they are executed depends on the actual implementation.
 */
interface Overseer : Closeable {

    /** List of outputs to be evaluated by the overseer. */
    val outputs: List<StreamOutput<out Any>>

    /**
     * Starts evaluating outputs according to desired execution strategy.
     *
     * @param sampleRate sample rate value to evaluate outputs with
     *
     * @return the list containing if the output was successfully evaluated.
     * Futures are resolved only when all outputs has been finished evaluation.
     */
    fun eval(sampleRate: Float): List<Future<Boolean>>
}

