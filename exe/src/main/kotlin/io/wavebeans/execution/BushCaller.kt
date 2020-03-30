package io.wavebeans.execution

import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.PodKey
import java.util.concurrent.Future

open class BushCallerRepository protected constructor(
        protected val podDiscovery: PodDiscovery
) {
    companion object {
        fun default(podDiscovery: PodDiscovery) = BushCallerRepository(podDiscovery)
    }

    open fun create(bushKey: BushKey, podKey: PodKey): BushCaller {
        return SimpleBushCaller(bushKey, podKey, podDiscovery)
    }
}

interface BushCaller {
    fun call(request: String): Future<PodCallResult>
}

class SimpleBushCaller internal constructor(
        val bushKey: BushKey,
        val podKey: PodKey,
        val podDiscovery: PodDiscovery
) : BushCaller {

    /***
     * @param request HTTP-like request: methodName?param1=value&param2=value
     */
    override fun call(request: String): Future<PodCallResult> {
        val bush = podDiscovery.bush(bushKey)
                ?: throw IllegalStateException("Unable to make call `$request` to Bush[$bushKey] as it hasn't been found.")
        // TODO that should be done over the network soon
//         if (true /* && bush.locallyAccessible()*/)
        return bush.call(podKey, request)
//        else {
//            TODO("call bush remotely")
//        }
    }
}