package mux.lib.execution

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

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

    companion object {
        val callPool = Executors.newFixedThreadPool(5)

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                callPool.shutdown()
                if (!callPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    callPool.shutdownNow()
                }
            })
        }
    }

    /***
     * @param request HTTP-like request: methodName?param1=value&param2=value
     */
    @ExperimentalStdlibApi
    override fun call(request: String): Future<PodCallResult> {
        val bush = podDiscovery.bush(bushKey)
                ?: throw IllegalStateException("Unable to make call `$request` to Bush[$bushKey] as it hasn't been found.")
        // TODO that should be done over the network soon
//         if (true /* && bush.locallyAccessible()*/)
        return callPool.submit(Callable<PodCallResult> { bush.call(podKey, request) })
//        else {
//            TODO("call bush remotely")
//        }
    }
}