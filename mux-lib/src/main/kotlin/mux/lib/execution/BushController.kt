package mux.lib.execution

import java.util.concurrent.Future

@ExperimentalStdlibApi
class BushController(val key: BushKey, val pods: List<PodRef>, threads: Int) {

    private val bush = Bush(key, threads)
            .also { b -> pods.forEach { b.addPod(it.instantiate()) } }

    fun start(): BushController {
        println("BUSH[$key] Started with pods=$pods")
        bush.start()
        return this
    }

    fun close() {
        println("BUSH[$key] Closed. ")
        bush.close()
    }

    fun getAllFutures(): List<Future<Boolean>> {
        return bush.tickPodsFutures()
    }
}
