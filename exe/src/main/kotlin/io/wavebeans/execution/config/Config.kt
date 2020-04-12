package io.wavebeans.execution.config

import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.medium.PodCallResultWithSerializationBuilder

object Config {

    /**
     * What builder to use build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun podCallResultBuilder(): PodCallResultBuilder = PodCallResultWithSerializationBuilder()

}