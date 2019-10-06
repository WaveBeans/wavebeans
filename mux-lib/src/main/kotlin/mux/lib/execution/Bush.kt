package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
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

    private val pods = ConcurrentHashMap<PodKey, Pod<*, *>>()

    private val bushKey = idSeq.incrementAndGet()

    private val queue = ConcurrentLinkedQueue<PodTask>()
    private val results = ConcurrentHashMap<Long, PodCallResult>()
    private val isStarted = AtomicBoolean(false)

    @ExperimentalStdlibApi
    private val bushThread = Thread {
        isStarted.set(true)
        while (isStarted.get()) {
            try {
                val task = queue.poll()
                if (task != null) {
                    val start = System.nanoTime()
                    println("Got task $task")
                    try {
                        val call = Call.parseRequest(task.request)
                        try {
                            val pod = pods[task.podKey] ?: TODO("pod not on bush case")

                            val result = pod.call(call)
                            results[task.taskId] = result

                            println("For task $task returned $result in ${(System.nanoTime() - start) / 1_000_000.0}ms")
                        } catch (e: Throwable) {
                            results[task.taskId] = PodCallResult.wrap(call, e)
                        }
                    } catch (e: Throwable) {
                        results[task.taskId] = PodCallResult.wrap(Call.empty, e)
                    }
                }
                sleep(0)
            } catch (e: InterruptedException) {
                println("Got interrupted")
                break
            }
        }
    }

    fun addPod(key: Int, pod: Pod<*, *>) {
        pods[key] = pod
        PodDiscovery.registerPod(bushKey, key, pod)
    }

    @ExperimentalStdlibApi
    fun start() {
        println("started $bushKey")
        PodDiscovery.registerBush(bushKey, this)
        bushThread.start()
        // wait until thread is started
        while (!isStarted.get()) {
            sleep(0)
        }
    }

    @ExperimentalStdlibApi
    override fun close() {
        PodDiscovery.unregisterBush(bushKey)
        pods.keys.forEach { PodDiscovery.unregisterPod(bushKey, it) }
        isStarted.set(false)
    }

    fun call(podKey: PodKey, request: String, timeout: Long = 1000L, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): PodCallResult {
        check(isStarted.get()) { "Bush is not started" }

        val start = System.nanoTime()
        val taskId = taskIdSeq.incrementAndGet()
        queue.add(PodTask(taskId, podKey, request))
        val nanoTimeout = TimeUnit.NANOSECONDS.convert(timeout, timeUnit)
        while (true) {
            val r = results[taskId]
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