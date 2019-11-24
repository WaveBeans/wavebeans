package mux.lib.execution

@ExperimentalStdlibApi
class BushController(val key: BushKey, val pods: List<PodRef>, threads: Int) {

    private val bush = Bush(key, threads)
            .also { b -> pods.forEach { b.addPod(it.instantiate()) } }

    fun start(): BushController {
        println("BUSH[$key] Started with pods=$pods")
        bush.start()
        return this
    }

    fun close(onlyFinished: Boolean) {
        val arePodsFinished = bush.areTickPodsFinished()
        if (onlyFinished && arePodsFinished || !onlyFinished) {
            println("BUSH[$key] Closed. Tick Pods were finished? `$arePodsFinished` ")
            bush.close()
        }
    }

    fun isFinished(): Boolean {
        return bush.areTickPodsFinished()
    }
}
