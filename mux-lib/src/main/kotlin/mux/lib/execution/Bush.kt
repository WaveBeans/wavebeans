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

class Bush : Closeable {

    companion object {
        private val idSeq = AtomicInteger(0)
        private val taskIdSeq = AtomicLong(0)
    }

    private val workers = ConcurrentHashMap<PodKey, PodWorker>()

    private val bushKey = idSeq.incrementAndGet()
            .also { PodDiscovery.registerBush(it, this) }


    fun addPod(key: Int, pod: Pod<*, *>) {
        val worker = PodWorker(pod)

        while (!worker.isStarted()) {
            sleep(0)
        }
        println("Pod started $key")

        workers[key] = worker
        PodDiscovery.registerPod(bushKey, key, pod)
    }

    @ExperimentalStdlibApi
    override fun close() {
        PodDiscovery.unregisterBush(bushKey)
        workers.forEach {
            PodDiscovery.unregisterPod(bushKey, it.key)
            it.value.close()

        }
    }

    fun call(podKey: PodKey, request: String, timeout: Long = 1000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): PodCallResult {
        val podWorker = workers[podKey] ?: TODO("pod not on the bush case")

        check(podWorker.isStarted()) { "PodWorker is not started" }

        val start = System.nanoTime()
        val taskId = taskIdSeq.incrementAndGet()
        val task = PodTask(taskId, podKey, request)
        println("Enqueued task $task")
        podWorker.enqueue(task)
        val nanoTimeout = TimeUnit.NANOSECONDS.convert(timeout, timeUnit)
        while (true) {
            val r = podWorker.result(taskId)
            if (r != null) {
                println("Call to pod[$podKey] for `$request` took ${(System.nanoTime() - start) / 1_000_000.0}ms")
                return r
            }
            if (start + nanoTimeout - System.nanoTime() <= 0)
                throw TimeoutException("Unable to call Pod with key=$podKey, request=`$request` within $timeout $timeUnit")
            sleep(0)
        }
    }
}