package io.wavebeans.execution.config

import io.wavebeans.execution.medium.*

object ExecutionConfig {

    private lateinit var podCallResultBuilder: PodCallResultBuilder

    private lateinit var mediumBuilder: MediumBuilder

    /**
     * What builder to use build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun podCallResultBuilder(): PodCallResultBuilder = podCallResultBuilder

    /**
     * What builder to use build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun mediumBuilder(): MediumBuilder = mediumBuilder

    /**
     * Initializes config for parallel processing mode
     */
    fun initForMultiThreadedProcessing() {
        podCallResultBuilder = PlainPodCallResultBuilder()
        mediumBuilder = PlainMediumBuilder()
    }

    /**
     * Initializes config for distributed processing mode
     */
    fun initForDistributedProcessing() {
        podCallResultBuilder = SerializingPodCallResultBuilder()
    }

}