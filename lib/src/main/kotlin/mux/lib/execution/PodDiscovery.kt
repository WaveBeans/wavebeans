package mux.lib.execution

import java.util.concurrent.ConcurrentHashMap

data class PodInfo(
        val bushKey: BushKey,
        val pod: Pod
)

open class PodDiscovery protected constructor() {

    companion object {
        val default = PodDiscovery()
    }

    private val bushes = ConcurrentHashMap<BushKey, Bush>()
    private val pods = ConcurrentHashMap<PodKey, PodInfo>()

    open fun bushFor(podKey: PodKey): BushKey {
        return pods[podKey]?.bushKey
                ?: throw IllegalStateException("Can't locate bush for pod with key $podKey")
    }

    open fun registerPod(bushKey: BushKey, pod: Pod) {
        val value = pods.putIfAbsent(pod.podKey, PodInfo(bushKey, pod))
        check(value == null) { "Pod with key `${pod.podKey}` already has value `$value`" }
    }

    open fun registerBush(bushKey: BushKey, bush: Bush) {
        val value = bushes.putIfAbsent(bushKey, bush)
        check(value == null) { "Bush with key `$bushKey` already has value `$value`" }
    }

    open fun bush(bushKey: BushKey): Bush? = bushes[bushKey]

    open fun unregisterBush(bushKey: BushKey) {
        bushes.remove(bushKey)
    }

    open fun unregisterPod(bushKey: BushKey, podKey: PodKey) {
        pods.remove(podKey)
    }
}