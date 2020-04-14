package io.wavebeans.execution.config

import io.wavebeans.execution.ExecutionThreadPool
import io.wavebeans.execution.MultiThreadedExecutionThreadPool
import io.wavebeans.execution.SingleThreadedExecutionThreadPool
import io.wavebeans.execution.medium.*
import mu.KotlinLogging

object ExecutionConfig {

    private val log = KotlinLogging.logger {}
    private lateinit var podCallResultBuilder: PodCallResultBuilder
    private lateinit var mediumBuilder: MediumBuilder
    private lateinit var executionThreadPool: ExecutionThreadPool

    /**
     * Number of threads to use for [executionThreadPool] for current JVM instance.
     */
    var threadsLimitForJvm: Int = 1
        set(value) {
            require(value > 0) { "Number of threads should be more than 0" }
            field = value
        }

    /**
     * Returns builder to use to build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun podCallResultBuilder(): PodCallResultBuilder = podCallResultBuilder

    /**
     * Returns builder to use to build [io.wavebeans.execution.medium.Medium]
     */
    fun mediumBuilder(): MediumBuilder = mediumBuilder

    /**
     * Returns shared thread pool to use for execution.
     */
    fun executionThreadPool(): ExecutionThreadPool {
        log.debug { "getting executionThreadPool. Stack Traces: ${Thread.currentThread().stackTrace.joinToString("\n")}" }
        return executionThreadPool
    }


    /**
     * Initializes config for simgle threaded processing mode
     */
    fun initForSingleThreadedProcessing() {
        podCallResultBuilder = PlainPodCallResultBuilder()
        mediumBuilder = PlainMediumBuilder()
        executionThreadPool = SingleThreadedExecutionThreadPool()
        log.info { "Initialized to work in single-threaded mode" }
    }

    /**
     * Initializes config for parallel processing mode
     */
    fun initForMultiThreadedProcessing() {
        podCallResultBuilder = PlainPodCallResultBuilder()
        mediumBuilder = PlainMediumBuilder()
        executionThreadPool = MultiThreadedExecutionThreadPool(threadsLimitForJvm)
        log.info { "Initialized to work in multi-threaded mode [threadsLimitForJvm=$threadsLimitForJvm]" }
    }
}