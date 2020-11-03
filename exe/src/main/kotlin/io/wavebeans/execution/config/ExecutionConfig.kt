package io.wavebeans.execution.config

import io.wavebeans.execution.ExecutionThreadPool
import io.wavebeans.execution.MultiThreadedExecutionThreadPool
import io.wavebeans.execution.medium.MediumBuilder
import io.wavebeans.execution.medium.PlainMediumBuilder
import io.wavebeans.execution.medium.PlainPodCallResultBuilder
import io.wavebeans.execution.medium.PodCallResultBuilder
import mu.KotlinLogging
import java.util.zip.Deflater

object ExecutionConfig {

    private val log = KotlinLogging.logger {}
    private lateinit var podCallResultBuilder: PodCallResultBuilder
    private lateinit var mediumBuilder: MediumBuilder
    private lateinit var executionThreadPool: ExecutionThreadPool

    /**
     * The size of the one partition for the pods. Must be the same across whole deployment.
     */
    var partitionSize: Int = 512

    /**
     * Number of buckets to prefetch at once while reading over iterator.
     */
    var prefetchBucketAmount = 10

    /**
     * Number of threads to use for [executionThreadPool] for current JVM instance.
     */
    var threadsLimitForJvm: Int = 1
        set(value) {
            require(value > 0) { "Number of threads should be more than 0" }
            field = value
        }

    /**
     * Whether use ZIP compression of the serialized objects during transfer. May put additional pressure on CPU
     */
    var serializationCompression = true

    /**
     * If [serializationCompression] enabled, that defines the level of compression 0-9:
     *  0: Compression level for no compression.
     *  1: Compression level for fastest compression.
     *  9: Compression level for best compression.
     */
    var serializationCompressionLevel = Deflater.BEST_COMPRESSION

    /**
     * When log is traced whether to also log the content of the buffer as hex doc.
     */
    var serializationLogTracing = false

    /**
     * Returns builder to use to build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun podCallResultBuilder(): PodCallResultBuilder = podCallResultBuilder

    /**
     * Sets builder to use to build [io.wavebeans.execution.medium.PodCallResult]
     */
    fun podCallResultBuilder(podCallResultBuilder: PodCallResultBuilder) {
        this.podCallResultBuilder = podCallResultBuilder
    }

    /**
     * Returns builder to use to build [io.wavebeans.execution.medium.Medium]
     */
    fun mediumBuilder(): MediumBuilder = mediumBuilder

    /**
     * Sets builder to use to build [io.wavebeans.execution.medium.Medium]
     */
    fun mediumBuilder(mediumBuilder: MediumBuilder) {
        this.mediumBuilder = mediumBuilder
    }

    /**
     * Returns shared thread pool to use for execution.
     */
    fun executionThreadPool(): ExecutionThreadPool {
        return executionThreadPool
    }

    /**
     * Sets shared thread pool to use for execution.
     */
    fun executionThreadPool(executionThreadPool: ExecutionThreadPool) {
        this.executionThreadPool = executionThreadPool
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