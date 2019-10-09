package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

typealias BushKey = Int

internal data class PodTask(
        val taskId: Long,
        val podKey: PodKey,
        val request: String
)

class Bush(
        val podDiscovery: PodDiscovery = PodDiscovery.default
) : Closeable {

    companion object {
        private val idSeq = AtomicInteger(0)
        private val taskIdSeq = AtomicLong(0)
    }

    private val workers = ConcurrentHashMap<PodKey, PodWorker>()

    private val bushKey = idSeq.incrementAndGet()
            .also { podDiscovery.registerBush(it, this) }


    @ExperimentalStdlibApi
    fun start() {
        workers.entries
                // tick pods should start last, as they start calling other pods before they've started
                .sortedBy { it.value.pod is TickPod }
                .forEach { (podKey, worker) ->
                    worker.start()
                    while (!worker.isStarted()) {
                        sleep(0)
                    }
                    println("Pod started [$podKey]${worker.pod}")
                }
    }

    fun addPod(pod: AnyPod) {
        val worker = PodWorker(pod)
        workers[pod.podKey] = worker
        podDiscovery.registerPod(bushKey, pod)
    }

    fun areTicksFinished(): Boolean {
        return workers.entries
                // tick pods should start last, as they start calling other pods before they've started
                .filter { it.value.pod is TickPod }
                .all { !it.value.isStarted() }
    }


    override fun close() {
        podDiscovery.unregisterBush(bushKey)
        workers.forEach {
            podDiscovery.unregisterPod(bushKey, it.key)
            it.value.close()
        }
    }

    fun call(podKey: PodKey, request: String, timeout: Long = 10000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): PodCallResult {
        val podWorker = workers[podKey] ?: TODO("pod not on the bush case")

        check(podWorker.isStarted()) { "PodWorker $podKey is not started. Can't do request `$request`" }

        val start = System.nanoTime()
        val taskId = taskIdSeq.incrementAndGet()
        val task = PodTask(taskId, podKey, request)
//        println("Enqueued task $task")
        podWorker.enqueue(task)
        var timeoutWarningFired = false
        val nanoTimeout = TimeUnit.NANOSECONDS.convert(timeout, timeUnit)
        while (true) {
            val r = podWorker.result(taskId)
            if (r != null) {
//                println("Call to pod[$podKey] for `$request` took ${(System.nanoTime() - start) / 1_000_000.0}ms")
                return r
            }
            if (!timeoutWarningFired && start + nanoTimeout / 2 - System.nanoTime() <= 0) {
                println("[WARNING] Long call to Pod [$podKey] for $request\n" +
                        Thread.currentThread().stackTrace.joinToString(separator = "\n") { it.toString() })
                timeoutWarningFired = true
            }
            if (start + nanoTimeout - System.nanoTime() <= 0)
                throw TimeoutException("Unable to call Pod with key=$podKey, request=`$request` within $timeout $timeUnit")
            sleep(0)
        }
    }
}