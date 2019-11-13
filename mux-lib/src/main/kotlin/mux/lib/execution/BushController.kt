package mux.lib.execution

@ExperimentalStdlibApi
class BushController {

    private val bushes = listOf(Bush())

    fun spreadThePods(pods: List<Pod>): BushController {
        // TODO make actual spreading
        pods.forEach { u ->
            bushes.first().addPod(u)
        }
        return this
    }

    fun start(): BushController {
        bushes.forEach(Bush::start)
        return this
    }

    fun close(onlyFinished: Boolean) {
        bushes
                .filter { onlyFinished && it.areTicksFinished() || !onlyFinished}
                .forEach { it.close() }
    }

    fun isFinished(): Boolean {
        return bushes.all { it.areTicksFinished() }
    }

}