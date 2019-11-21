package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalStdlibApi
class Overseer : Closeable {

    companion object {
        val bushKeySeq = AtomicInteger(0)
    }

    private val controllers = mutableListOf<BushController>()

    fun deployTopology(topology: Topology): Overseer {
        // master node makes planning and submits to certain bushes
        val pods = PodBuilder(topology).build()
        println("Pods: $pods")
        // bush controller spreads pods over one or few bushes.
        controllers += BushController(bushKeySeq.incrementAndGet(), pods)
                .start()
        println("OVERSEER All controllers (${controllers.size}) are started")
        return this
    }

    fun waitToFinish(delay: Long = 1000): Overseer {
        while (!controllers.all { it.isFinished() }) {
            sleep(delay)
        }
        println("OVERSEER All controllers (${controllers.size}) are finished")
        return this
    }

    override fun close() {
        controllers.forEach { it.close(false) }
        println("OVERSEER All controllers (${controllers.size}) are closed")
    }

}