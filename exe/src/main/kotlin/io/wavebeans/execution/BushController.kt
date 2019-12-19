package io.wavebeans.execution

import mu.KotlinLogging
import java.util.concurrent.Future

@ExperimentalStdlibApi
class BushController(val key: BushKey, val pods: List<PodRef>, threads: Int, sampleRate: Float) {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val bush = Bush(key, threads)
            .also { b -> pods.forEach { b.addPod(it.instantiate(sampleRate)) } }

    fun start(): BushController {
        log.info { "BUSH[$key] Started with pods=$pods" }
        bush.start()
        return this
    }

    fun close() {
        bush.close()
        log.info { "BUSH[$key] Closed. " }
    }

    fun getAllFutures(): List<Future<Boolean>> {
        return bush.tickPodsFutures()
    }
}
