package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class Overseer : Closeable {

    private val controllers = mutableListOf<BushController>()

    fun deployTopology(topology: Topology): Overseer {
        // master node makes planning and submits to certain bushes
        val pods = PodBuilder(topology).build()
        println("Pods: $pods")
        // bush controller spreads pods over one or few bushes.
        controllers += BushController()
                .spreadThePods(pods)
                .start()
        return this
    }

    fun waitToFinish(delay: Long = 1000): Overseer {
        while (!controllers.all { it.isFinished() }) {
            sleep(delay)
        }
        return this
    }

    override fun close() {
        controllers.forEach { it.close(false) }
    }

}