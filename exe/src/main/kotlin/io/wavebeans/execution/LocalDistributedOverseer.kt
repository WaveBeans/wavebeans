package io.wavebeans.execution

import io.wavebeans.lib.io.StreamOutput
import mu.KotlinLogging
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalStdlibApi
class LocalDistributedOverseer(
        override val outputs: List<StreamOutput<out Any>>,
        private val threadsCount: Int,
        private val partitionsCount: Int
) : Overseer {

    companion object {
        val bushKeySeq = AtomicInteger(0)
        private val log = KotlinLogging.logger { }
    }

    private val controllers = mutableListOf<BushController>()

    private val topology = outputs.buildTopology()
            .partition(partitionsCount)
            .groupBeans()

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        log.info { "Deploying topology: ${TopologySerializer.serialize(topology, TopologySerializer.jsonPretty)}" }
        val pods = PodBuilder(topology).build()
        log.info { "Pods: $pods" }
        controllers += BushController(bushKeySeq.incrementAndGet(), pods, threadsCount, sampleRate)
                .start()
        log.info { "All controllers (amount=${controllers.size}) are started" }

        return controllers.flatMap { it.getAllFutures() }
    }

    override fun close() {
        controllers.forEach { it.close() }
        log.info { "All controllers (amount=${controllers.size}) are closed" }
    }

}