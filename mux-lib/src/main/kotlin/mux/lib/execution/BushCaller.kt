package mux.lib.execution

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
    fun call(request: String): PodCallResult
}

class SimpleBushCaller internal constructor(
        val bushKey: BushKey,
        val podKey: PodKey,
        val podDiscovery: PodDiscovery
) : BushCaller {

    /***
     * @param request HTTP-like request: methodName?param1=value&param2=value
     */
    @ExperimentalStdlibApi
    override fun call(request: String): PodCallResult {
        val bush = podDiscovery.bush(bushKey)
        // TODO that should be done over the network soon
        return if (bush != null /* && bush.locallyAccessible()*/)
            bush.call(podKey, request)
        else {
            TODO("call bush remotely")
        }
    }
}