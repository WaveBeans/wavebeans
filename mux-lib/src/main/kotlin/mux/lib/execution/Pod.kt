package mux.lib.execution

import mux.lib.*
import java.lang.reflect.InvocationTargetException

typealias PodKey = Int
typealias AnyPod = Pod<*, *>

interface Pod<T : Any, S : Any> : Bean<T, S> {

    val podKey: PodKey

    @ExperimentalStdlibApi
    fun call(call: Call): PodCallResult {
        return try {
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
            PodCallResult.wrap(call, result)
        } catch (e: InvocationTargetException) {
            PodCallResult.wrap(call, e.targetException)
        } catch (e: Throwable) {
            PodCallResult.wrap(call, e)
        }
    }
}

interface PodProxy<T : Any, S : Any> : Bean<T, S> {

    val pointedTo: PodKey
}

