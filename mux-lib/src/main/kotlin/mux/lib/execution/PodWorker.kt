package mux.lib.execution

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class PodWorker(private val pod: Pod<*, *>) : Closeable {
    private val results = ConcurrentHashMap<Long, PodCallResult>()

    private val queue = ConcurrentLinkedQueue<PodTask>()

    private val isStarted = AtomicBoolean(false)

    @ExperimentalStdlibApi
    private val podThread = Thread {
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
                Thread.sleep(0)
            } catch (e: InterruptedException) {
                println("Got interrupted")
                break
            }
        }
    }.also(Thread::start)

    fun isStarted(): Boolean = isStarted.get()

    override fun close() {
        isStarted.set(false)
    }

    internal fun enqueue(task: PodTask) {
        queue.add(task)
    }

    internal fun result(taskId: Long): PodCallResult? = results[taskId]
}