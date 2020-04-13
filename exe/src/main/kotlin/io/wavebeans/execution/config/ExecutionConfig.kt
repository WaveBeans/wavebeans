package io.wavebeans.execution.config

import io.wavebeans.execution.medium.PlainPodCallResultBuilder
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.medium.SerializingPodCallResultBuilder

object ExecutionConfig {

    private lateinit var podCallResultBuilder: PodCallResultBuilder

    /**
     * What builder to use build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun podCallResultBuilder(): PodCallResultBuilder = podCallResultBuilder

    /**
     * Initializes config for parallel processing mode
     */
    fun initForParallelProcessing() {
        podCallResultBuilder = PlainPodCallResultBuilder()
    }

    /**
     * Initializes config for distributed processing mode
     */
    fun initForDistributedProcessing() {
        podCallResultBuilder = SerializingPodCallResultBuilder()
    }

}