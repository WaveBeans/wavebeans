package io.wavebeans.execution

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalStdlibApi
class Overseer : Closeable {

    companion object {
        val bushKeySeq = AtomicInteger(0)
    }

    private val controllers = mutableListOf<BushController>()

    fun deployTopology(topology: Topology, threadsPerBush: Int): Overseer {
        // master node makes planning and submits to certain bushes
        val pods = PodBuilder(topology).build()
        println("Pods: $pods")
        // bush controller spreads pods over one or few bushes.
        controllers += BushController(bushKeySeq.incrementAndGet(), pods, threadsPerBush)
                .start()
        println("OVERSEER All controllers (${controllers.size}) are started")
        return this
    }

    fun waitToFinish(): Overseer {
        controllers.map { it.getAllFutures() }.flatten().all { it.get() == true }
        println("OVERSEER All controllers (${controllers.size}) are finished")
        return this
    }

    override fun close() {
        controllers.forEach { it.close() }
        println("OVERSEER All controllers (${controllers.size}) are closed")
    }

}