package mux.lib.execution

import java.io.Closeable
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.typeOf

typealias BushKey = Int

class Bush(
        val bushKey: BushKey,
        val threadsCount: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default
) : Closeable {

    class NamedThreadFactory(val name: String) : ThreadFactory {
        private var c = 0
        override fun newThread(r: Runnable): Thread {
            return Thread(ThreadGroup(name), r, "$name-${c++}")
        }
    }

    private val workingPool = Executors.newFixedThreadPool(threadsCount, NamedThreadFactory("work"))

    @Volatile
    private var isDraining = false

    private val pods = ConcurrentHashMap<PodKey, Pod>()

    private val tickFinished = ConcurrentHashMap<Pod, Boolean>()

    init {
        podDiscovery.registerBush(bushKey, this)
    }

    inner class Tick(val pod: TickPod) : Runnable {

        override fun run() {
            try {
                if (!isDraining && pod.tick()) {
                    workingPool.submit(Tick(pod))
                } else {
                    tickFinished[pod] = true
                }
            } catch (e: Exception) {
                workingPool.submit(Tick(pod))
                e.printStackTrace(System.err)
            }
        }

    }

    @ExperimentalStdlibApi
    fun start() {
        pods.values.filterIsInstance<TickPod>()
                .map { pod ->
                    tickFinished[pod] = false
                    workingPool.submit(Tick(pod))
                }
    }

    fun addPod(pod: Pod) {
        pods[pod.podKey] = pod
        podDiscovery.registerPod(bushKey, pod)
    }

    fun areTickPodsFinished(): Boolean {
        // TODO what if no tick pods on bush?
        // TODO handle failure finish
        return tickFinished.values.all { it }
    }


    override fun close() {
        isDraining = true
        pods.values.forEach {
            it.close()
            podDiscovery.unregisterPod(bushKey, it.podKey)
        }
        workingPool.shutdown()
        if (!workingPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            workingPool.shutdownNow()
        }
        podDiscovery.unregisterBush(bushKey)
    }

    @ExperimentalStdlibApi
    fun call(podKey: PodKey, request: String): Future<PodCallResult> {
        val pod = pods[podKey]
        check(pod != null) { "Pod $podKey is not found on Bush $bushKey" }
        val call = Call.parseRequest(request)
        return when (call.method) {
            "iteratorStart" -> {
                val iteratorKey = pod.iteratorStart(call.param("sampleRate", typeOf<Float>()) as Float, call.param("partitionIdx", typeOf<Int>()) as Int)
                val res = CompletableFuture<PodCallResult>()
                res.complete(PodCallResult.wrap(call, iteratorKey))
                res
            }
            "iteratorNext" -> {
                val iteratorKey = call.param("iteratorKey", typeOf<Long>()) as Long
                val buckets = call.param("buckets", typeOf<Int>()) as Int
                val res = CompletableFuture<PodCallResult>()
                val r = try {
                    PodCallResult.wrap(call, pod.iteratorNext(iteratorKey, buckets))
                } catch (e: Throwable) {
                    PodCallResult.wrap(call, e)
                }
                res.complete(r)
                res
            }
            else -> throw UnsupportedOperationException("$call can't be handled")
        }
    }
}