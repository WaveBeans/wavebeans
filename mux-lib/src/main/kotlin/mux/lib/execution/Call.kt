package mux.lib.execution

import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class Call(
        val method: String,
        val params: Map<String, String>
) {
    companion object {

        val empty = Call("", emptyMap())

        fun parseRequest(request: String): Call {
            val split = request.split("?", limit = 2)
            val method = split[0]
            val q = split.elementAtOrNull(1)
            val params = q?.split("&")
                    ?.map { it.split("=", limit = 2) }
                    ?.map { it[0] to it[1] }
                    ?.toMap()
                    ?: emptyMap()
            return Call(method, params)
        }
    }

    @ExperimentalStdlibApi
    fun param(key: String, type: KType): Any? {
        return when (type) {
            typeOf<Int>() -> params[key]?.toInt()
            typeOf<Float>() -> params[key]?.toFloat()
            typeOf<Long>() -> params[key]?.toLong()
            else -> throw UnsupportedOperationException("$type is unsupported during call to `$method`")
        }
    }
}