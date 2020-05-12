package io.wavebeans.execution

import io.wavebeans.execution.pod.PodKey
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

open class PodDiscovery protected constructor() {

    companion object {
        val default = PodDiscovery()
        val log = KotlinLogging.logger { }
    }

    private val bushes = ConcurrentHashMap<BushKey, Bush>()
    private val pods = ConcurrentHashMap<PodKey, BushKey>()

    open fun bushFor(podKey: PodKey): BushKey {
        val bushKey = pods[podKey] ?: throw IllegalStateException("Can't locate bush for pod with key $podKey")
        log.debug { "Requested bush for $podKey: $bushKey" }
        return bushKey
    }

    open fun registerPod(bushKey: BushKey, podKey: PodKey) {
        log.debug { "Registering pod $podKey on Bush $bushKey" }
        val value = pods.putIfAbsent(podKey, bushKey)
        check(value == null) { "Pod with key `$podKey` already has value `$value`" }
    }

    open fun registerBush(bushKey: BushKey, bush: Bush) {
        log.debug { "Registered bush $bush as $bushKey" }
        val value = bushes.putIfAbsent(bushKey, bush)
        check(value == null) { "Bush with key `$bushKey` already has value `$value`" }
    }

    open fun bush(bushKey: BushKey): Bush? = bushes[bushKey].also {
        log.debug { "Got bush for key $bushKey: $it" }
    }

    open fun unregisterBush(bushKey: BushKey) {
        log.debug { "Unregistered bush $bushKey" }
        bushes.remove(bushKey)
    }

    open fun unregisterPod(bushKey: BushKey, podKey: PodKey) {
        log.debug { "Unregistered pod $podKey on bush $bushKey" }
        pods.remove(podKey)
    }
}