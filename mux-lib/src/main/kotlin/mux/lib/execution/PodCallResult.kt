package mux.lib.execution

import mux.lib.Sample
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

// TODO whole approach should be revisited

class PodCallResult(val call: Call, val byteArray: ByteArray?, val exception: Throwable?) {
    companion object {
        fun wrap(call: Call, value: Any?): PodCallResult {
            return PodCallResult(
                    call = call,
                    byteArray = when (value) {
                        null -> null
                        is Unit -> null
                        is Long -> value.encodeBytes()
                        is Sample -> value.encodeBytes()
                        is List<*> -> value.encodeBytes()
                        else -> throw UnsupportedOperationException("Value `$value` is not supported")
                    },
                    exception = null
            )
        }

        fun wrap(call: Call, exception: Throwable): PodCallResult = PodCallResult(call, null, exception)
    }

    override fun toString(): String {
        return "PodCallResult(call=$call, byteArray[${byteArray?.size}]=${byteArray?.take(10).toString()}, exception=$exception)"
    }

}

internal fun Long.encodeBytes(): ByteArray =
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeLong(this)
            }
            baos.toByteArray()
        }


fun PodCallResult.long(): Long =
        this.throwIfError().byteArray?.let { ba ->
            ObjectInputStream(ByteArrayInputStream(ba)).use { ois ->
                ois.readLong()
            }

        } ?: throw IllegalStateException("Can't decode null", this.exception)


internal fun Sample.encodeBytes(): ByteArray = this.toBits().encodeBytes()

fun PodCallResult.sample(): Sample = Double.fromBits(this.long())

internal fun <T : Any> encodeList(list: List<T>, elementEncoder: (ObjectOutputStream, T) -> Unit): ByteArray =
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeInt(list.size)
                list.forEach {
                    elementEncoder(oos, it)
                }
            }
            baos.toByteArray()
        }


internal fun encodeEmptyList(): ByteArray =
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeInt(0)
            }
            baos.toByteArray()
        }


internal fun List<*>.encodeBytes(): ByteArray =
        if (isEmpty()) {
            encodeEmptyList()
        } else {
            val value = this.first()
            when (value) {
                is Sample -> encodeList(this as List<Sample>) { oos, el -> oos.writeDouble(el) }
                else -> throw UnsupportedOperationException("${value!!::class}")
            }
        }


fun PodCallResult.sampleList(): List<Sample> =
        throwIfError().byteArray?.let { ba ->
            ObjectInputStream(ByteArrayInputStream(ba)).use { ois ->
                val elementsCount = ois.readInt()

                (0 until elementsCount).map { ois.readDouble() }.toList()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)

fun PodCallResult.nullableSampleList(): List<Sample>? =
        ifNotNull { this.sampleList() }

internal fun PodCallResult.throwIfError(): PodCallResult =
        if (this.exception != null) throw IllegalStateException("Got exception during call ${this.call}", this.exception)
        else this


internal fun <T> PodCallResult.ifNotNull(function: () -> T): T? =
        throwIfError()
                .byteArray?.let { function() }

