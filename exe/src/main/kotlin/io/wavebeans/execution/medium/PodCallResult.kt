package io.wavebeans.execution.medium

import io.wavebeans.execution.Call
import io.wavebeans.lib.Sample
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.WindowStreamParams
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

// TODO whole approach should be revisited

class PodCallResult(val call: Call, val byteArray: ByteArray?, val exception: Throwable?, val type: String) {
    companion object {
        fun wrap(call: Call, value: Any?): PodCallResult {
            val (byteArray, type) = when (value) {
                null -> Pair(null, "Null")
                is Unit -> Pair(null, "Unit")
                is Long -> Pair(value.encodeBytes(), "Long")
                is Sample -> Pair(value.encodeBytes(), "Sample")
                is List<*> -> value.encodeBytesAndType()
                else -> throw UnsupportedOperationException("Value `$value` is not supported")
            }
            return PodCallResult(
                    call = call,
                    byteArray = byteArray,
                    exception = null,
                    type = type
            )
        }

        fun wrap(call: Call, exception: Throwable): PodCallResult = PodCallResult(call, null, exception, "Exception")
    }

    fun isNull(): Boolean = this.byteArray == null && this.exception == null

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


@Suppress("UNCHECKED_CAST")
internal fun List<*>.encodeBytesAndType(): Pair<ByteArray, String> =
        if (isEmpty()) {
            Pair(encodeEmptyList(), "EmptyList")
        } else {
            when (val value = this.first()) {
                is Sample -> {
                    Pair(
                            encodeList(this as List<Sample>, 8) { buf, at, el -> writeLong(el.toBits(), buf, at) },
                            "List<Sample>"
                    )
                }
                is SampleArray -> { // List<Array<Double>>
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
                    Pair(buf, "List<SampleArray>")
                }
                is WindowSampleArray -> { //List<WindowSampleArray>
                    val list = this.map { it as WindowSampleArray }

                    val contentSize = list.map {
                        4 + // innerArray.size
                                it.sampleArray.map { innerArr ->
                                    4 + // elementsArray.size
                                            4 + // window size
                                            4 + // window step
                                            innerArr.size * 8 // elements
                                }.sum()
                    }.sum()

                    val buf = ByteArray(4 + contentSize)
                    var pointer = 0

                    writeInt(this.size, buf, pointer); pointer += 4

                    list.forEach { innerArr ->
                        writeInt(innerArr.windowSize, buf, pointer); pointer += 4
                        writeInt(innerArr.windowStep, buf, pointer); pointer += 4
                        writeInt(innerArr.sampleArray.size, buf, pointer); pointer += 4
                        innerArr.sampleArray.forEach { sampleArray ->
                            writeInt(sampleArray.size, buf, pointer); pointer += 4
                            repeat(sampleArray.size) {
                                writeLong(sampleArray[it].toBits(), buf, pointer); pointer += 8
                            }
                        }
                    }

                    Pair(buf, "List<WindowSampleArray>")
                }
                is Array<*> -> { // List<Array<*>>
                    if (this.isEmpty()) {
                        Pair(encodeEmptyList(), "EmptyList")
                    } else {
                        when (val aval = value[0]) {
                            is FftSample -> {
                                val list = this.map { it as Array<FftSample> }

                                val contentSize = list.map {
                                    4 + // innerArray.size
                                            it.map { el -> el.encodeSize() }.sum()
                                }.sum()

                                val buf = ByteArray(4 + contentSize)
                                var pointer = 0

                                writeInt(this.size, buf, pointer); pointer += 4

                                list.forEach { innerArray ->
                                    writeInt(innerArray.size, buf, pointer); pointer += 4
                                    innerArray.forEach { fftSample ->
                                        pointer = fftSample.encode(buf, pointer)
                                    }
                                }

                                Pair(buf, "List<Array<FftSample>>")
                            }
                            else -> throw UnsupportedOperationException("${aval!!::class}")
                        }
                    }
                }
                else -> throw UnsupportedOperationException("${value!!::class}")
            }
        }


internal fun FftSample.encode(buf: ByteArray, at: Int): Int {
    var pointer = at
    writeLong(this.time, buf, pointer); pointer += 8
    writeInt(this.binCount, buf, pointer); pointer += 4
    writeInt(this.sampleRate.toBits(), buf, pointer); pointer += 4
    writeInt(this.fft.size, buf, pointer); pointer += 4
    this.fft.forEach {
        writeLong(it.re.toBits(), buf, pointer); pointer += 8
        writeLong(it.im.toBits(), buf, pointer); pointer += 8
    }
    return pointer
}

internal fun decodeFftSample(buf: ByteArray, from: Int): Pair<FftSample, Int> {
    var pointer = from
    val time = readLong(buf, pointer); pointer += 8
    val binCount = readInt(buf, pointer); pointer += 4
    val sampleRate = Float.fromBits(readInt(buf, pointer)); pointer += 4
    val fftSize = readInt(buf, pointer); pointer += 4
    val fft = ArrayList<ComplexNumber>(fftSize)
    repeat(fftSize) {
        val re = Double.fromBits(readLong(buf, pointer)); pointer += 8
        val im = Double.fromBits(readLong(buf, pointer)); pointer += 8
        fft.add(ComplexNumber(re, im))
    }

    return Pair(
            FftSample(time, binCount, sampleRate, fft),
            pointer
    )
}

internal fun FftSample.encodeSize(): Int =
        8 + // time [Long]
                4 + // binCount [Int]
                4 + // sampleRate [Float]
                4 + // fft.size [Int]
                this.fft.size *
                ( // fft [List<ComplexNumber>]
                        8 + // re [Double]
                                8  // im [Double]
                        )


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

fun PodCallResult.nullableSampleList(): List<Sample>? = ifNotNull { this.sampleList() }

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


fun PodCallResult.windowSampleArrayList(): List<WindowSampleArray> =
        throwIfError().byteArray?.let { buf ->
            var pointer = 0
            val size = readInt(buf, pointer); pointer += 4
            if (size > 0) {
                val list = ArrayList<WindowSampleArray>(size)
                repeat(size) {
                    val windowSize = readInt(buf, pointer); pointer += 4
                    val windowStep = readInt(buf, pointer); pointer += 4
                    val innerArraySize = readInt(buf, pointer); pointer += 4
                    list.add(WindowSampleArray(windowSize, windowStep, Array(innerArraySize) {
                        val sampleArraySize = readInt(buf, pointer); pointer += 4
                        createSampleArray(sampleArraySize) {
                            val bits = readLong(buf, pointer); pointer += 8
                            Double.fromBits(bits)
                        }
                    }))
                }
                list
            } else {
                emptyList<WindowSampleArray>()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)


fun PodCallResult.fftSampleArrayList(): List<FftSampleArray> =
        throwIfError().byteArray?.let { buf ->
            var pointer = 0

            val size = readInt(buf, pointer); pointer += 4
            if (size > 0) {
                val list = ArrayList<FftSampleArray>(size)
                repeat(size) {
                    val fftSampleArraySize = readInt(buf, pointer); pointer += 4
                    list.add(createFftSampleArray(fftSampleArraySize) {
                        val (fftSample, newPointer) = decodeFftSample(buf, pointer)
                        pointer = newPointer
                        fftSample
                    })
                }
                list
            } else {
                emptyList<FftSampleArray>()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)


fun PodCallResult.nullableSampleArrayList(): List<SampleArray>? = ifNotNull { this.sampleArrayList() }
fun PodCallResult.nullableFftSampleArrayList(): List<FftSampleArray>? = ifNotNull { this.fftSampleArrayList() }
fun PodCallResult.nullableWindowSampleArrayList(): List<WindowSampleArray>? = ifNotNull { this.windowSampleArrayList() }