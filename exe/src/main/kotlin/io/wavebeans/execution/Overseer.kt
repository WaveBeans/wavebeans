package io.wavebeans.execution

import io.wavebeans.execution.TopologySerializer.jsonPretty
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalStdlibApi
class Overseer : Closeable {

    companion object {
        val bushKeySeq = AtomicInteger(0)
        private val log = KotlinLogging.logger { }
    }

    private val controllers = mutableListOf<BushController>()

    fun deployTopology(topology: Topology, threadsPerBush: Int, sampleRate: Float): Overseer {
        // master node makes planning and submits to certain bushes
        log.info { "Deploying topology: ${TopologySerializer.serialize(topology, jsonPretty)}" }
        val pods = PodBuilder(topology).build()
        log.info { "Pods: $pods" }
        // bush controller spreads pods over one or few bushes.
        controllers += BushController(bushKeySeq.incrementAndGet(), pods, threadsPerBush, sampleRate)
                .start()
        log.info { "All controllers (amount=${controllers.size}) are started" }
        return this
    }

    fun waitToFinish(): Overseer {
        controllers.map { it.getAllFutures() }.flatten().all { it.get() == true }
        log.info { "All controllers (amount=${controllers.size}) are finished" }
        return this
    }

    override fun close() {
        controllers.forEach { it.close() }
        log.info { "All controllers (amount=${controllers.size}) are closed" }
    }

}