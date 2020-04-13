package io.wavebeans.execution.medium

import io.wavebeans.execution.Call
import io.wavebeans.lib.Sample
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.stream.fft.FftSample
import kotlin.reflect.KClass

// TODO whole approach should be revisited

class SerializingPodCallResultBuilder : PodCallResultBuilder {
    override fun ok(call: Call, value: Any?): PodCallResult {
        TODO()
//        if (value == null) return PodCallResultWithSerialization(call, null, null, "Null")
//        if (value is Unit) return PodCallResultWithSerialization(call, null, null, "Unit")
//        val mediumCreator = MediumRegistry.creatorFor(value::class)
//        val medium = mediumCreator(value)
//        val serializer = medium.serializer()
//        val buf = ByteArray(serializer.size())
//        serializer.writeTo(buf, 0)
//        return PodCallResultWithSerialization(
//                call = call,
//                byteArray = buf,
//                exception = null,
//                type = serializer.type()
//        )
    }

    override fun error(call: Call, exception: Throwable): PodCallResult {
        return SerializazingPodCallResult(call, null, exception, "Exception")
    }

}

class SerializazingPodCallResult(
        override val call: Call,
        val byteArray: ByteArray?,
        override val exception: Throwable?,
        val type: String
) : PodCallResult {

    override fun isNull(): Boolean = this.byteArray == null && this.exception == null

    override fun toString(): String {
        return "PodCallResultWithSerialization(call=$call, byteArray[${byteArray?.size}]=${byteArray?.take(10).toString()}, exception=$exception)"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(clazz: KClass<T>): T? {
        return when (clazz) {
            Long::class -> this.decodeLong() as T
            List::class -> when(this.type) {
                else -> throw UnsupportedOperationException("List of type ${this.type} is not recognized")
            }
            else -> throw UnsupportedOperationException("$clazz is not recognized")
        }
    }

}

fun SerializazingPodCallResult.decodeLong(): Long =
        this.throwIfError().byteArray?.let { readLong(it, 0) }
                ?: throw IllegalStateException("Can't decode null", this.exception)


fun SerializazingPodCallResult.sample(): Sample = Double.fromBits(this.decodeLong())

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
                is Medium -> {
                    val list = this as List<Medium>
                    val serializers = list.map { it.serializer() }
                    val sizeOfAllElements = serializers.map { it.size() }.sum()
                    val buf = ByteArray(4 + sizeOfAllElements)
                    var offset = 0
                    writeInt(list.size, buf, 0); offset += 4
                    serializers.forEach {
                        it.writeTo(buf, offset); offset += it.size()
                    }
                    Pair(
                            buf,
                            "List<${list.first().serializer().type()}"
                    )
                }
//                is SampleMedium -> { // List<Array<Double>>
//                    val seq = this.asSequence().map { (it as SampleMedium) }
//
//                    val contentSize = seq.map { 4 + it.size * 8 }.sum()
//                    var pointer = 0
//                    val buf = ByteArray(4 + contentSize)
//                    writeInt(this.size, buf, 0)
//                    pointer += 4
//                    seq.forEach { el ->
//                        writeInt(el.size, buf, pointer)
//                        pointer += 4
//                        repeat(el.size) {
//                            writeLong(el[it].toBits(), buf, pointer)
//                            pointer += 8
//                        }
//                    }
//                    Pair(buf, "List<SampleArray>")
//                }
//                is WindowSampleMedium -> { //List<WindowSampleArray>
//                    val list = this.map { it as WindowSampleMedium }
//
//                    val contentSize = list.map {
//                        4 + // innerArray.size
//                                it.sampleArray.map { innerArr ->
//                                    4 + // elementsArray.size
//                                            4 + // window size
//                                            4 + // window step
//                                            innerArr.size * 8 // elements
//                                }.sum()
//                    }.sum()
//
//                    val buf = ByteArray(4 + contentSize)
//                    var pointer = 0
//
//                    writeInt(this.size, buf, pointer); pointer += 4
//
//                    list.forEach { innerArr ->
//                        writeInt(innerArr.windowSize, buf, pointer); pointer += 4
//                        writeInt(innerArr.windowStep, buf, pointer); pointer += 4
//                        writeInt(innerArr.sampleArray.size, buf, pointer); pointer += 4
//                        innerArr.sampleArray.forEach { sampleArray ->
//                            writeInt(sampleArray.size, buf, pointer); pointer += 4
//                            repeat(sampleArray.size) {
//                                writeLong(sampleArray[it].toBits(), buf, pointer); pointer += 8
//                            }
//                        }
//                    }
//
//                    Pair(buf, "List<WindowSampleArray>")
//                }
//                is Array<*> -> { // List<Array<*>>
//                    if (this.isEmpty()) {
//                        Pair(encodeEmptyList(), "EmptyList")
//                    } else {
//
//                        data class ListArrayElEncoder(
//                                val elName: String,
//                                val sizeEncoder: (Any) -> Int,
//                                val elEncoder: (Any, ByteArray, Int) -> Int
//                        )
//
//                        val elEncoder = when (val aval = value[0]) {
//                            is FftSample -> {
//                                ListArrayElEncoder(
//                                        "FftSample",
//                                        { (it as FftSample).encodeSize() },
//                                        { el, buf, pointer -> (el as FftSample).encode(buf, pointer) }
//                                )
//                            }
//                            is DoubleArray -> {
//                                ListArrayElEncoder(
//                                        "DoubleArray",
//                                        {
//                                            4 + // array.size
//                                                    (it as DoubleArray).size * 8 // size of all elements
//                                        },
//                                        { el, buf, pointer ->
//                                            var p = pointer
//                                            val a = el as DoubleArray
//                                            writeInt(a.size, buf, p); p += 4
//                                            a.forEach { writeLong(it.toBits(), buf, p); p += 8 }
//                                            p
//                                        }
//                                )
//                            }
//                            else -> throw UnsupportedOperationException("${aval!!::class}")
//                        }
//
//                        val contentSize = this.map {
//                            4 + // innerArray.size
//                                    (it as Array<Any>).map { el -> elEncoder.sizeEncoder(el) }.sum()
//                        }.sum()
//                        val buf = ByteArray(4 + contentSize)
//                        var pointer = 0
//
//                        writeInt(this.size, buf, pointer); pointer += 4
//
//                        this.forEach {
//                            val innerArray = it as Array<Any>
//                            writeInt(innerArray.size, buf, pointer); pointer += 4
//                            innerArray.forEach { el -> pointer = elEncoder.elEncoder(el, buf, pointer) }
//                        }
//                        Pair(buf, "List<Array<${elEncoder.elName}>>")
//                    }
//                }
                else -> throw UnsupportedOperationException("${value!!::class}")
            }
        }


internal fun FftSample.encode(buf: ByteArray, at: Int): Int {
    var pointer = at
    writeLong(this.index, buf, pointer); pointer += 8
    writeInt(this.binCount, buf, pointer); pointer += 4
    writeInt(this.samplesCount, buf, pointer); pointer += 4
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
    val index = readLong(buf, pointer); pointer += 8
    val binCount = readInt(buf, pointer); pointer += 4
    val samplesCount = readInt(buf, pointer); pointer += 4
    val sampleRate = Float.fromBits(readInt(buf, pointer)); pointer += 4
    val fftSize = readInt(buf, pointer); pointer += 4
    val fft = ArrayList<ComplexNumber>(fftSize)
    repeat(fftSize) {
        val re = Double.fromBits(readLong(buf, pointer)); pointer += 8
        val im = Double.fromBits(readLong(buf, pointer)); pointer += 8
        fft.add(ComplexNumber(re, im))
    }

    return Pair(
            FftSample(index, binCount, samplesCount, sampleRate, fft),
            pointer
    )
}

internal fun FftSample.encodeSize(): Int =
        8 + // index [Long]
                4 + // binCount [Int]
                4 + // samplesCount [Int]
                4 + // sampleRate [Float]
                4 + // fft.size [Int]
                this.fft.size *
                ( // fft [List<ComplexNumber>]
                        8 + // re [Double]
                                8  // im [Double]
                        )


fun SerializazingPodCallResult.sampleList(): List<Sample> =
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

fun SerializazingPodCallResult.nullableSampleList(): List<Sample>? = ifNotNull { this.sampleList() }

internal fun SerializazingPodCallResult.throwIfError(): SerializazingPodCallResult =
        if (this.exception != null) throw IllegalStateException("Got exception during call ${this.call}", this.exception)
        else this


internal fun <T> SerializazingPodCallResult.ifNotNull(function: () -> T): T? =
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

fun SerializazingPodCallResult.sampleArrayList(): List<SampleMedium> = TODO()
//        throwIfError().byteArray?.let { buf ->
//            var pointer = 0
//            val size = readInt(buf, 0)
//            pointer += 4
//            if (size > 0) {
//                val list = ArrayList<SampleMedium>(size)
//                repeat(size) {
//                    val arraySize = readInt(buf, pointer)
//                    pointer += 4
//                    list.add(createSampleArray(arraySize) {
//                        val bits = readLong(buf, pointer)
//                        val v = Double.fromBits(bits)
//                        pointer += 8
//                        v
//                    })
//                }
//                list
//            } else {
//                emptyList<SampleMedium>()
//            }
//        } ?: throw IllegalStateException("Can't decode null", this.exception)


fun SerializazingPodCallResult.windowSampleArrayList(): List<WindowSampleMedium> = TODO()
//        throwIfError().byteArray?.let { buf ->
//            var pointer = 0
//            val size = readInt(buf, pointer); pointer += 4
//            if (size > 0) {
//                val list = ArrayList<WindowSampleMedium>(size)
//                repeat(size) {
//                    val windowSize = readInt(buf, pointer); pointer += 4
//                    val windowStep = readInt(buf, pointer); pointer += 4
//                    val innerArraySize = readInt(buf, pointer); pointer += 4
//                    list.add(WindowSampleMedium(windowSize, windowStep, Array(innerArraySize) {
//                        val sampleArraySize = readInt(buf, pointer); pointer += 4
//                        createSampleArray(sampleArraySize) {
//                            val bits = readLong(buf, pointer); pointer += 8
//                            Double.fromBits(bits)
//                        }
//                    }))
//                }
//                list
//            } else {
//                emptyList<WindowSampleMedium>()
//            }
//        } ?: throw IllegalStateException("Can't decode null", this.exception)


fun SerializazingPodCallResult.fftSampleArrayList(): List<FftSampleArrayMedium> =
        throwIfError().byteArray?.let { buf ->
            var pointer = 0

            val size = readInt(buf, pointer); pointer += 4
            if (size > 0) {
                val list = ArrayList<FftSampleArrayMedium>(size)
                repeat(size) {
//                    val fftSampleArraySize = readInt(buf, pointer); pointer += 4
//                    list.add(createFftSampleArray(fftSampleArraySize) {
//                        val (fftSample, newPointer) = decodeFftSample(buf, pointer)
//                        pointer = newPointer
//                        fftSample
//                    })
                }
                list
            } else {
                emptyList<FftSampleArrayMedium>()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)

fun SerializazingPodCallResult.doubleArrayArrayList(): List<Array<DoubleArray>> =
        throwIfError().byteArray?.let { buf ->
            var pointer = 0

            val size = readInt(buf, pointer); pointer += 4
            if (size > 0) {
                val list = ArrayList<Array<DoubleArray>>(size)
                repeat(size) {
                    val doubleArrayArraySize = readInt(buf, pointer); pointer += 4
                    list.add(Array<DoubleArray>(doubleArrayArraySize) {
                        val doubleArraySize = readInt(buf, pointer); pointer += 4
                        DoubleArray(doubleArraySize) {
                            val v = readLong(buf, pointer); pointer += 8
                            Double.fromBits(v)
                        }
                    })
                }
                list
            } else {
                emptyList<Array<DoubleArray>>()
            }
        } ?: throw IllegalStateException("Can't decode null", this.exception)


fun SerializazingPodCallResult.nullableSampleArrayList(): List<SampleMedium>? = ifNotNull { this.sampleArrayList() }
fun SerializazingPodCallResult.nullableFftSampleArrayList(): List<FftSampleArrayMedium>? = ifNotNull { this.fftSampleArrayList() }
fun SerializazingPodCallResult.nullableWindowSampleArrayList(): List<WindowSampleMedium>? = ifNotNull { this.windowSampleArrayList() }
fun SerializazingPodCallResult.nullableDoubleArrayArrayList(): List<Array<DoubleArray>>? = ifNotNull { this.doubleArrayArrayList() }

