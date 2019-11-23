package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.*

typealias BushKey = Int

class Bush(
        val bushKey: BushKey,
        val podDiscovery: PodDiscovery = PodDiscovery.default
) : Closeable {

    private val pool = Executors.newFixedThreadPool(8)
    private val tickPool = Executors.newCachedThreadPool()

    @Volatile
    private var isDraining = false

    private val pods = ConcurrentHashMap<PodKey, Pod>()

    private val tickFutures = CopyOnWriteArrayList<Future<Boolean>>()

    init {
        podDiscovery.registerBush(bushKey, this)
    }

    @ExperimentalStdlibApi
    fun start() {
        tickFutures += pods.values.filterIsInstance<TickPod>()
                .map { pod ->
                    tickPool.submit(Callable {
                        return@Callable try {
                            while (!isDraining && pod.tick()) sleep(0)
                            println("TICK POD [$pod] finished successfully [isDraining=$isDraining]")
                            true
                        } catch (e: Exception) {
                            System.err.println("TICK POD [$pod] finished with exception: $e")
                            e.printStackTrace()
                            false
                        }

                    })
                }
    }

    fun addPod(pod: Pod) {
        pods[pod.podKey] = pod
        podDiscovery.registerPod(bushKey, pod)
    }

    fun areTickPodsFinished(): Boolean {
        // TODO what if no tick pods on bush?
        // TODO handle failure finish
        return tickFutures.all { it.isDone }
    }


    override fun close() {
        isDraining = true
        pods.values.forEach {
            it.close()
            podDiscovery.unregisterPod(bushKey, it.podKey)
        }
        pool.shutdown()
        if (!pool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            pool.shutdownNow()
        }
        tickPool.shutdown()
        if (!tickPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            tickPool.shutdownNow()
        }
        podDiscovery.unregisterBush(bushKey)
    }

    @ExperimentalStdlibApi
    fun call(podKey: PodKey, request: String, timeout: Long = 1000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): PodCallResult {
        val pod = pods[podKey]
        check(pod != null) { "Pod $podKey is not found on Bush $bushKey" }
        val callable = Callable<PodCallResult> {
            val call = Call.parseRequest(request)
            if (!isDraining) {
                val start = System.nanoTime()
                try {
                    val result = pod.call(call)

                    val l = (System.nanoTime() - start) / 1_000_000.0
                    if (l > 100) println("[WARNING] [POD:$pod] For request $request returned $result in ${l}ms")

                    result
                } catch (e: Throwable) {
                    PodCallResult.wrap(call, e)
                }
            } else {
                PodCallResult.wrap(call, null)
            }
        }
        val f = pool.submit(callable)
        return f.get(/*timeout, timeUnit*/)

    }
}