package io.wavebeans.execution.pod

import io.wavebeans.execution.Call
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.lib.AnyBean
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.Closeable
import java.lang.reflect.InvocationTargetException

@Serializable
data class PodKey(val id: Long, val partition: Int)

interface Pod : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    val podKey: PodKey

    fun inputs(): List<AnyBean>

    fun call(call: Call): PodCallResult {
        return try {
            val start = System.nanoTime()
            val method = this::class.members
                    .firstOrNull { it.name == call.method }
                    ?: throw IllegalStateException("[$this] Can't find method to call: $call")
            val params = method.parameters
                    .drop(1) // drop self instance
                    .map {
                        call.param(
                                key = it.name
                                        ?: throw IllegalStateException("[$this] Parameter `$it` of method `$method` has no name"),
                                type = it.type)
                    }
                    .toTypedArray()

            val result = method.call(this, *params)

            val callTime = (System.nanoTime() - start) / 1_000_000.0
            if (callTime > 50) log.warn { "[$this][call=$call] Call time ${callTime}ms" }


            val r = PodCallResult.ok(call, result)
            val resultTime = (System.nanoTime() - start) / 1_000_000.0 - callTime
            if (resultTime > 50) log.warn { "[$this][call=$call] Result wrap time ${resultTime}ms" }

            r
        } catch (e: InvocationTargetException) {
            PodCallResult.error(call, e.targetException)
        } catch (e: Throwable) {
            if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
            PodCallResult.error(call, e)
        }
    }

    fun start() {}

    fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long

    fun iteratorNext(iteratorKey: Long, buckets: Int): List<Any>?

    fun isFinished(): Boolean

    fun desiredSampleRate(): Float?
}

