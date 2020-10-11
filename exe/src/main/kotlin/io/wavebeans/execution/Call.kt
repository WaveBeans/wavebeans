package io.wavebeans.execution

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.concurrent.TimeUnit
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
data class Call(
        @ProtoNumber(1)
        val method: String,
        @ProtoNumber(2)
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

    fun param(key: String, type: KType): Any? {
        return when (type) {
            typeOf<Int>() -> params[key]?.toInt()
            typeOf<Float>() -> params[key]?.toFloat()
            typeOf<Long>() -> params[key]?.toLong()
            typeOf<Boolean>() -> params[key]?.toBoolean()
            typeOf<TimeUnit>() -> params[key]?.let { TimeUnit.valueOf(it) }
            else -> throw UnsupportedOperationException("$type is unsupported during call to `$method`")
        }
    }
}