package io.wavebeans.execution

import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.pod.TickPod
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.*

typealias BushKey = Int

data class ExecutionResult(val finished: Boolean, val exception: Exception?) {
    companion object{
        fun success() = ExecutionResult(true,null)
        fun error(e: Exception) = ExecutionResult(false, e)
    }
}

class Bush(
        val bushKey: BushKey,
        val threadsCount: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default
) : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

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

    private val tickFinished = ConcurrentHashMap<Pod, CompletableFuture<ExecutionResult>>()

    init {
        podDiscovery.registerBush(bushKey, this)
    }

    inner class Tick(val pod: TickPod) : Runnable {

        override fun run() {
            try {
                if (!isDraining && pod.tick()) {
                    workingPool.submit(Tick(pod))
                } else {
                    tickFinished[pod]!!.complete(ExecutionResult.success())
                }
            } catch (e: Exception) {
                tickFinished[pod]!!.complete(ExecutionResult.error(e))
            }
        }

    }

    @ExperimentalStdlibApi
    fun start() {
        pods.values.filterIsInstance<TickPod>()
                .map { pod ->
                    tickFinished[pod] = CompletableFuture()
                    pod.start()
                    pod
                }.map { pod ->
                    workingPool.submit(Tick(pod))
                }
    }

    fun addPod(pod: Pod) {
        pods[pod.podKey] = pod
        podDiscovery.registerPod(bushKey, pod)
    }

    fun tickPodsFutures(): List<Future<ExecutionResult>> {
        return tickFinished.values.toList()
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
        val res = CompletableFuture<PodCallResult>()
        val r = try {
            val retVal = pod.call(call)
            retVal
        } catch (e: Throwable) {
            PodCallResult.wrap(call, e)
        }
        res.complete(r)
        return res
    }
}