package mux.lib.execution

import mux.lib.Sample

class PodCallResult(val call: Call, val byteArray: ByteArray?, val exception: Throwable?) {
    companion object {
        fun wrap(call: Call, value: Any?): PodCallResult {
            return PodCallResult(
                    call = call,
                    byteArray = when (value) {
                        null -> null
                        is Long -> value.encodeBytes()
                        is Sample -> value.encodeBytes()
                        is Unit -> null
                        else -> throw UnsupportedOperationException("Value `$value` is not supported")
                    },
                    exception = null
            )
        }

        fun wrap(call: Call, exception: Throwable): PodCallResult = PodCallResult(call, null, exception)
    }

    override fun toString(): String {
        return "PodCallResult(call=$call, byteArray=${byteArray?.contentToString()}, exception=$exception)"
    }
}

internal fun Long.encodeBytes(): ByteArray = this.toString().toByteArray()

fun PodCallResult.long(): Long =
        String(this.throwIfError().byteArray
                ?: throw IllegalStateException("Can't decode null", this.exception)
        ).toLong()


internal fun Sample.encodeBytes(): ByteArray = this.toBits().encodeBytes()

fun PodCallResult.sample(): Sample = Double.fromBits(this.long())


internal fun PodCallResult.throwIfError(): PodCallResult {
    if (this.exception != null) throw IllegalStateException("Got exception during call ${this.call}", this.exception)
    return this
}

internal fun <T> PodCallResult.ifNotNull(function: () -> T): T? = this.byteArray?.let { function() }
