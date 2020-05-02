package io.wavebeans.execution

import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.pod.TickPod
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import kotlin.math.abs
import kotlin.random.Random

class LocalBush(
        override val bushKey: BushKey,
        val podDiscovery: PodDiscovery = PodDiscovery.default
) : Bush {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val workingPool = ExecutionConfig.executionThreadPool()

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
                    log.debug { "Tick pod $pod has finished as it is over [isDraining=$isDraining]" }
                    tickFinished[pod]!!.complete(ExecutionResult.success())
                }
            } catch (e: Throwable) {
                if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
                log.debug(e) { "Tick pod $pod has finished due to error" }
                tickFinished[pod]!!.complete(ExecutionResult.error(e))
            }
        }

    }

    override fun start() {
        pods.values.filterIsInstance<TickPod>()
                .map { pod ->
                    tickFinished[pod] = CompletableFuture()
                    pod.start()
                    pod
                }.map { pod ->
                    workingPool.submit(Tick(pod))
                }
    }

    override fun addPod(pod: Pod) {
        pods[pod.podKey] = pod
        podDiscovery.registerPod(bushKey, pod.podKey)
    }

    override fun pods(): List<Pod> = pods.values.toList()

    override fun tickPodsFutures(): List<Future<ExecutionResult>> {
        return tickFinished.values.toList()
    }


    override fun close() {
        isDraining = true
        pods.values.forEach {
            it.close()
            podDiscovery.unregisterPod(bushKey, it.podKey)
        }
        podDiscovery.unregisterBush(bushKey)
    }

    override fun call(podKey: PodKey, request: String): Future<PodCallResult> {
        val pod = pods[podKey]
        check(pod != null) { "Pod $podKey is not found on Bush $bushKey" }
        val call = Call.parseRequest(request)
        val res = CompletableFuture<PodCallResult>()
        val id = abs(Random.nextInt()).toString(16)
        val start = System.currentTimeMillis()
        val r = try {
            log.trace { "[$id][$this] Calling pod=$podKey, request=$request" }
            val retVal = pod.call(call)
            log.trace { "[$id][$this] Call to pod=$podKey, request=$request took ${System.currentTimeMillis() - start}ms" }
            retVal
        } catch (e: Throwable) {
            if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
            log.info(e) {
                "[$id][$this] Call to pod=$podKey, request=$request " +
                        "took ${System.currentTimeMillis() - start}ms and ended up with error"
            }
            PodCallResult.error(call, e)
        }
        res.complete(r)
        return res
    }
}