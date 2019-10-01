package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal data class PodTask(
        val taskId: Long,
        val podKey: PodKey,
        val request: String
)

class Bush : Closeable {

    class Result(val byteArray: ByteArray?)

    companion object {
        private val idSeq = AtomicInteger(0)
        private val taskIdSeq = AtomicLong(0)
    }

    private val pods = ConcurrentHashMap<PodKey, Pod<*, *>>()

    private val bushKey = idSeq.incrementAndGet()

    private val queue = ConcurrentLinkedQueue<PodTask>()
    private val results = ConcurrentHashMap<Long, Result>()

    @ExperimentalStdlibApi
    private val bushThread = Thread {
        while (true) {
            try {
                val task = queue.poll()
                if (task != null) {
                    val pod = pods[task.podKey] ?: TODO("pod not on bush case")

                    val call = Call.parseRequest(task.request)

                    val method = pod::class.members
                            .firstOrNull { it.name == call.method }
                            ?: throw IllegalStateException("Can't find method to call: $task")
                    val params = method.parameters.map { call.param(it.name!!, it.type) }.toTypedArray()

                    results[task.taskId] = Result(pod.call(method, params))

                }
                sleep(1)
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
        PodDiscovery.registerBush(bushKey, this)
        bushThread.start()
    }

    @ExperimentalStdlibApi
    override fun close() {
        bushThread.interrupt()
    }

    fun call(podKey: PodKey, request: String): ByteArray? {
        val taskId = taskIdSeq.incrementAndGet()
        queue.add(PodTask(taskId, podKey, request))
        while (true) {
            val r = results[taskId]
            if (r != null) return r.byteArray
            sleep(1)
        }
    }

}