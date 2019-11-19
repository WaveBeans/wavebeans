package mux.lib.execution

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class PodWorker(val pod: Pod) : Closeable {

    private val results = ConcurrentHashMap<Long, PodCallResult>()

    private val queue = ConcurrentLinkedQueue<PodTask>()

    private val isStarted = AtomicBoolean(false)

    @ExperimentalStdlibApi
    private val podThreads =
            if (pod is SplittingPod) (0 until pod.partitionCount).map { createThread(pod) }
            else listOf(createThread(pod))

    @ExperimentalStdlibApi
    private fun createThread(pod: Pod): Thread {
        return Thread({
            isStarted.set(true)
            var i = 0
            while (true) {
                try {
                    if (pod is TickPod) {
                        if (!pod.tick()) {
                            println("Pod $pod has ended his work")
                            break
                        }
                    } else {
                        val task = queue.poll()
                        if (task != null) {
                            val start = System.nanoTime()
                            //                        println("Got task $task")
                            try {
                                val call = Call.parseRequest(task.request)
                                try {
                                    val result = pod.call(call)
                                    results[task.taskId] = result

                                    val l = (System.nanoTime() - start) / 1_000_000.0
                                    if (l > 100) println("[WARNING] [POD:$pod] For task $task returned $result in ${l}ms")
                                } catch (e: Throwable) {
                                    results[task.taskId] = PodCallResult.wrap(call, e)
                                }
                            } catch (e: Throwable) {
                                results[task.taskId] = PodCallResult.wrap(Call.empty, e)
                            }
                        }
                    }
                    if (i++ % 1000 == 0) {
                        i = 0
                        if (!isStarted.get()) break
                    }
                    Thread.sleep(0)
                } catch (e: InterruptedException) {
                    println("Got interrupted")
                    break
                } catch (e: Throwable) {
                    println("[ERROR]<Pod: $pod> Unexpected exception ${e.message}")
                    e.printStackTrace()
                    break
                }
            }
            isStarted.set(false)
        }, pod.toString())
    }

    @ExperimentalStdlibApi
    fun start() {
        podThreads.forEach { it.start() }
    }

    fun isStarted(): Boolean = isStarted.get()

    override fun close() {
        pod.close()
        isStarted.set(false)
    }

    internal fun enqueue(task: PodTask) {
        queue.add(task)
    }

    internal fun result(taskId: Long): PodCallResult? = results.remove(taskId)
}