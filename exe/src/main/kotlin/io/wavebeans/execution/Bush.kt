package io.wavebeans.execution

import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import java.io.Closeable
import java.util.*
import java.util.concurrent.Future

typealias BushKey = UUID

fun newBushKey(): BushKey = UUID.randomUUID()
fun String.toBushKey(): BushKey = UUID.fromString(this)

data class ExecutionResult(val finished: Boolean, val exception: Throwable?) {
    companion object {
        fun success() = ExecutionResult(true, null)
        fun error(e: Throwable) = ExecutionResult(false, e)
    }
}

interface Bush : Closeable {

    val bushKey: BushKey

    fun start()

    fun addPod(pod: Pod)

    fun pods(): List<Pod>

    fun tickPodsFutures(): List<Future<ExecutionResult>>

    fun call(podKey: PodKey, request: String): Future<PodCallResult>

}