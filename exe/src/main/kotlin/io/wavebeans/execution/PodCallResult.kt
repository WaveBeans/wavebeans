package io.wavebeans.execution

import io.wavebeans.lib.Sample
import io.wavebeans.lib.SampleArray
import io.wavebeans.lib.createSampleArray
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

internal fun <T : Any> encodeList(list: List<T>, elementSize: Int, elementWriter: (ByteArray, Int, T) -> Unit): ByteArray {
    val buf = ByteArray(4 + elementSize * list.size)
    writeInt(list.size, buf, 0)
    list.forEachIndexed { index, t ->
        elementWriter(buf, 4 + index * elementSize, t)
    }
    return buf
}


internal fun encodeEmptyList(): ByteArray {
    val buf = ByteArray(4)
    writeInt(0, buf, 0)
    return buf
}


internal fun List<*>.encodeBytes(): ByteArray =
        if (isEmpty()) {
            encodeEmptyList()
        } else {
            when (val value = this.first()) {
                is Sample -> {
                    encodeList(this as List<Sample>, 8) { buf, at, el -> writeLong(el.toBits(), buf, at) }
                }
                is SampleArray -> {
                    val seq = this.asSequence().map { (it as SampleArray) }

                    val contentSize = seq.map { 4 + it.size * 8 }.sum()

                    var pointer = 0
                    val buf = ByteArray(4 + contentSize)
                    writeInt(this.size, buf, 0)
                    pointer += 4
                    seq.forEach { el ->
                        writeInt(el.size, buf, pointer)
                        pointer += 4
                        repeat(el.size) {
                            writeLong(el[it].toBits(), buf, pointer)
                            pointer += 8
                        }
                    }
                    buf
                }
                else -> throw UnsupportedOperationException("${value!!::class}")
            }
        }


fun PodCallResult.sampleList(): List<Sample> =
        throwIfError().byteArray?.let { buf ->
            val size = readInt(buf, 0)
            if (size > 0) {
                val list = ArrayList<Sample>(size)
                (0 until size).forEach {
                    list.add(Double.fromBits(readLong(buf, 4 + it * 8)))
                }
                list
            } else {
                emptyList<Sample>()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)

fun PodCallResult.sampleArrayList(): List<SampleArray> =
        throwIfError().byteArray?.let { buf ->
            var pointer = 0
            val size = readInt(buf, 0)
            pointer += 4
            if (size > 0) {
                val list = ArrayList<SampleArray>(size)
                repeat(size) {
                    val arraySize = readInt(buf, pointer)
                    pointer += 4
                    list.add(createSampleArray(arraySize) {
                        val bits = readLong(buf, pointer)
                        val v = Double.fromBits(bits)
                        pointer += 8
                        v
                    })
                }
                list
            } else {
                emptyList<SampleArray>()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)

fun PodCallResult.nullableSampleList(): List<Sample>? = ifNotNull { this.sampleList() }
fun PodCallResult.nullableSampleArrayList(): List<SampleArray>? = ifNotNull { this.sampleArrayList() }

internal fun PodCallResult.throwIfError(): PodCallResult =
        if (this.exception != null) throw IllegalStateException("Got exception during call ${this.call}", this.exception)
        else this


internal fun <T> PodCallResult.ifNotNull(function: () -> T): T? =
        throwIfError()
                .byteArray?.let { function() }

internal fun writeLong(value: Long, buf: ByteArray, at: Int) {
    check(buf.size - at >= 8) { "Buffer is not enough to fit Long value (${buf.size - at} bytes left)" }
    writeInt((value and 0xFFFFFFFF).toInt(), buf, at)
    writeInt(((value ushr 32) and 0xFFFFFFFF).toInt(), buf, at + 4)
}

internal fun readLong(buf: ByteArray, from: Int): Long {
    return readInt(buf, from).toLong() and 0xFFFFFFFF or
            (readInt(buf, from + 4).toLong() shl 32)
}

internal fun writeInt(value: Int, buf: ByteArray, at: Int) {
    check(buf.size - at >= 4) { "Buffer is not enough to fit Int value (${buf.size - at} bytes left)" }
    buf[at + 0] = (value and 0xFF).toByte()
    buf[at + 1] = (value ushr 8 and 0xFF).toByte()
    buf[at + 2] = (value ushr 16 and 0xFF).toByte()
    buf[at + 3] = (value ushr 24 and 0xFF).toByte()
}

internal fun readInt(buf: ByteArray, from: Int): Int {
    return (buf[from + 0].toInt() and 0xFF) or
            (buf[from + 1].toInt() and 0xFF shl 8) or
            (buf[from + 2].toInt() and 0xFF shl 16) or
            (buf[from + 3].toInt() and 0xFF shl 24)
}

