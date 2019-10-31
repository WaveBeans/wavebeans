package mux.lib.execution

import mux.lib.*
import java.lang.reflect.InvocationTargetException

typealias PodKey = Int
typealias AnyPod = Pod<*, *>
typealias AnyPodProxy = PodProxy<*, *>

interface Pod<T : Any, S : Any> : Bean<T, S> {

    val podKey: PodKey
    val partition: Int

    @ExperimentalStdlibApi
    fun call(call: Call): PodCallResult {
        return try {
            val start = System.nanoTime()
            val method = this::class.members
                    .firstOrNull { it.name == call.method }
                    ?: throw IllegalStateException("Can't find method to call: $call")
            val params = method.parameters
                    .drop(1) // drop self instance
                    .map {
                        call.param(
                                key = it.name
                                        ?: throw IllegalStateException("Parameter `$it` of method `$method` has no name"),
                                type = it.type)
                    }
                    .toTypedArray()

            val result = method.call(this, *params)

            val callTime = (System.nanoTime() - start) / 1_000_000.0
            if (callTime > 50) println("[$this][call=$call] Call time ${callTime}ms")

            val r = PodCallResult.wrap(call, result)
            val resultTime = (System.nanoTime() - start) / 1_000_000.0 - callTime
            if (resultTime > 50) println("[$this][call=$call] Result wrap time ${resultTime}ms")

            r
        } catch (e: InvocationTargetException) {
            PodCallResult.wrap(call, e.targetException)
        } catch (e: Throwable) {
            PodCallResult.wrap(call, e)
        }
    }
}

interface PodProxy<T : Any, S : Any> : Bean<T, S> {

    val pointedTo: PodKey
    val partition: Int
}

