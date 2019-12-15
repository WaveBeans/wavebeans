package io.wavebeans.execution.pod

import io.wavebeans.execution.Call
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.lib.AnyBean
import mu.KotlinLogging
import java.io.Closeable
import java.lang.reflect.InvocationTargetException

data class PodKey(val id: Int, val partition: Int)

interface Pod : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    val podKey: PodKey

    fun inputs(): List<AnyBean>

    @ExperimentalStdlibApi
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

            val r = PodCallResult.wrap(call, result)
            val resultTime = (System.nanoTime() - start) / 1_000_000.0 - callTime
            if (resultTime > 50) log.warn { "[$this][call=$call] Result wrap time ${resultTime}ms" }

            r
        } catch (e: InvocationTargetException) {
            PodCallResult.wrap(call, e.targetException)
        } catch (e: Throwable) {
            PodCallResult.wrap(call, e)
        }
    }

    fun start() {}

    fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long

    fun iteratorNext(iteratorKey: Long, buckets: Int): List<Any>?

    fun isFinished(): Boolean
}

