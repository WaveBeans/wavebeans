package mux.lib.execution

import java.util.concurrent.ConcurrentHashMap

data class PodInfo(
        val key: PodKey,
        val bushKey: BushKey,
        val endpoint: AnyPodEndpoint
)

object PodDiscovery {

    private val bushes = ConcurrentHashMap<BushKey, Bush>()
    private val pods = ConcurrentHashMap<PodKey, PodInfo>()

    fun bushFor(podEndpointKey: PodKey): Sequence<BushKey> {
        return pods.asSequence().filter { it.value.key == podEndpointKey }.map { it.value.bushKey }
    }

    fun registerPod(bushKey: BushKey, podKey: PodKey, podEndpoint: AnyPodEndpoint) {
        val value = pods.putIfAbsent(podKey, PodInfo(podKey, bushKey, podEndpoint))
        check(value == null) { "Pod with key `$podKey` already has value `$value`" }
    }

    fun registerBush(bushKey: BushKey, bush: Bush) {
        val value = bushes.putIfAbsent(bushKey, bush)
        check(value == null) { "Bush with key `$bushKey` already has value `$value`" }
    }

    fun bush(bushKey: BushKey): Bush? = bushes[bushKey]

    fun endpoints(): Sequence<PodInfo> = pods.values.asSequence()

}