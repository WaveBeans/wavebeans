package io.wavebeans.execution.pod

/**
 * [Pod] that wait for ticks from [Bush] in order to perform some work.
 */
interface TickPod : Pod {

    /**
     * Performs a tick.
     *
     * @return `true` if pod needs continue working and awaits for more ticks, or `false` if pod finished his job and awaits to be closed
     */
    fun tick(): Boolean
}