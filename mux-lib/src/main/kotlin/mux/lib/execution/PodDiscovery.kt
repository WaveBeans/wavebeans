package mux.lib.execution

import java.util.concurrent.ConcurrentHashMap

data class PodInfo(
        val key: PodKey,
        val bushKey: BushKey,
        val pod: AnyPod
)

object PodDiscovery {

    private val bushes = ConcurrentHashMap<BushKey, Bush>()
    private val pods = ConcurrentHashMap<PodKey, PodInfo>()

    fun bushFor(podKey: PodKey): BushKey {
        return pods.asSequence().first { it.value.key == podKey }.value.bushKey
    }

    fun registerPod(bushKey: BushKey, podKey: PodKey, pod: AnyPod) {
        val value = pods.putIfAbsent(podKey, PodInfo(podKey, bushKey, pod))
        check(value == null) { "Pod with key `$podKey` already has value `$value`" }
    }
    fun registerBush(bushKey: BushKey, bush: Bush) {
        val value = bushes.putIfAbsent(bushKey, bush)
        check(value == null) { "Bush with key `$bushKey` already has value `$value`" }
    }

    fun bush(bushKey: BushKey): Bush? = bushes[bushKey]

    fun pods(): Sequence<PodInfo> = pods.values.asSequence()

    fun unregisterBush(bushKey: BushKey) {
        bushes.remove(bushKey)
    }

    fun unregisterPod(bushKey: BushKey, podKey: PodKey) {
        pods.remove(podKey)
    }
}