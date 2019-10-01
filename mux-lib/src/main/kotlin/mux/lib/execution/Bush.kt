package mux.lib.execution

import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal data class PodEndpointTask(
        val taskId: Long,
        val podEndpointKey: PodKey,
        val request: String
)

class Bush : Closeable {

    class Result(val byteArray: ByteArray?)

    companion object {
        private val idSeq = AtomicInteger(0)
        private val taskIdSeq = AtomicLong(0)
    }

    private val podEndpoints = ConcurrentHashMap<PodKey, PodEndpoint<*, *>>()

    private val bushKey = idSeq.incrementAndGet()

    private val queue = ConcurrentLinkedQueue<PodEndpointTask>()
    private val results = ConcurrentHashMap<Long, Result>()

    @ExperimentalStdlibApi
    private val bushThread = Thread {
        while (true) {
            try {
                val task = queue.poll()
                if (task != null) {
                    val podEndpoint = podEndpoints[task.podEndpointKey] ?: TODO("pod not on bush case")

                    val call = Call.parseRequest(task.request)

                    val endpointMethod = podEndpoint::class.members
                            .firstOrNull { it.name == call.method }
                            ?: throw IllegalStateException("Can't find method to call: $task")
                    val params = endpointMethod.parameters.map { call.param(it.name!!, it.type) }.toTypedArray()

                    results[task.taskId] = Result(podEndpoint.call(endpointMethod, params))

                }
                sleep(1)
            } catch (e: InterruptedException) {
                println("Got interrupted")
                break
            }
        }
    }

    fun addPodEndpoint(key: Int, endpoint: PodEndpoint<*, *>) {
        podEndpoints[key] = endpoint
        PodDiscovery.registerPod(bushKey, key, endpoint)
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

    fun call(podEndpointKey: Int, request: String): ByteArray? {
        val taskId = taskIdSeq.incrementAndGet()
        queue.add(PodEndpointTask(taskId, podEndpointKey, request))
        while (true) {
            val r = results[taskId]
            if (r != null) return r.byteArray
            sleep(1)
        }
    }

}