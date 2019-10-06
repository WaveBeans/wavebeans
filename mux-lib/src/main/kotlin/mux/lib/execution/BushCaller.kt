package mux.lib.execution

class BushCaller(val bushKey: BushKey, val podKey: PodKey) {

    /***
     * @param request HTTP-like request: methodName?param1=value&param2=value
     */
    fun call(request: String): PodCallResult {
        val bush = PodDiscovery.bush(bushKey)
        // TODO that should be done over the network soon
        return if (bush != null /* && bush.locallyAccessible()*/)
            bush.call(podKey, request)
        else {
            TODO("call bush bush remotely")
        }
    }
}