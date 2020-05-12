package io.wavebeans.execution

import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.lib.io.StreamOutput
import mu.KotlinLogging
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Launches whole topology on specified number of threads. Some beans are being partitioned if they support that.
 * All beans are being evaluated in parallel, the order of execution is not guaranteed.
 */
class MultiThreadedOverseer(
        override val outputs: List<StreamOutput<out Any>>,
        private val threadsCount: Int,
        private val partitionsCount: Int
) : Overseer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val controllers = mutableListOf<Gardener>()

    private val jobKey = newJobKey()

    private val topology = outputs.buildTopology()
            .partition(partitionsCount)
            .groupBeans()

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        ExecutionConfig.threadsLimitForJvm = threadsCount
        ExecutionConfig.initForMultiThreadedProcessing()
        log.info { "Deploying topology: ${TopologySerializer.serialize(topology, TopologySerializer.jsonPretty)}" }
        val pods = PodBuilder(topology).build()
        log.info { "Pods: $pods" }
        controllers += Gardener()
                .plantBush(jobKey, newBushKey(), pods, sampleRate)
                .start(jobKey)
        log.info { "All controllers (amount=${controllers.size}) are started" }

        return controllers.flatMap { it.getAllFutures(jobKey) }
    }

    override fun close() {
        controllers.forEach { it.stop(jobKey) }
        log.info { "All controllers (amount=${controllers.size}) are closed" }
        ExecutionConfig.executionThreadPool().shutdown()
        if (!ExecutionConfig.executionThreadPool().awaitTermination(10000, TimeUnit.MILLISECONDS)) {
            ExecutionConfig.executionThreadPool().shutdownNow()
        }
        log.info { "Execution thread pool is closed" }
    }

}