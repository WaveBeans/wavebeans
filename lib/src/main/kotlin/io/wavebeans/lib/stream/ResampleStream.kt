package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mu.KotlinLogging
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.truncate
import kotlin.reflect.jvm.jvmName

@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        to: Float? = null,
        resampleFn: (ResamplingArgument<Sample>) -> Sequence<Sample>
): BeanStream<Sample> {
    return this.resample(to, Fn.wrap(resampleFn))
}

@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>> = sincResampleFunc()
): BeanStream<Sample> {
    return ResampleStream(this, ResampleStreamParams(to, resampleFn))
}

fun <T : Any> BeanStream<T>.resample(
        to: Float? = null,
        resampleFn: (ResamplingArgument<T>) -> Sequence<T>
): BeanStream<T> {
    return this.resample(to, Fn.wrap(resampleFn))
}

fun <T : Any> BeanStream<T>.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<T>, Sequence<T>> = SimpleResampleFn {
            throw IllegalStateException("Using ${SimpleResampleFn::class} as a " +
                    "resample function, but reduce function is not defined")
        }
): BeanStream<T> {
    return ResampleStream(this, ResampleStreamParams(to, resampleFn))
}

@Serializable
data class ResamplingArgument<T>(
        val inputSampleRate: Float,
        val outputSampleRate: Float,
        val inputOutputScaleFactor: Float,
        val inputSequence: Sequence<T>
)

fun sincResampleFunc(windowSize: Int = 32): SincResampleFn<Sample, SampleVector> {
    return SincResampleFn(
            windowSize = windowSize,
            createVectorFn = { (size, iterator) ->
                sampleVectorOf(size) { _, _ ->
                    if (iterator.hasNext()) iterator.next() else ZeroSample
                }
            },
            extractNextVectorFn = { a ->
                val (size, offset, window, iterator) = a
                sampleVectorOf(size) { i, n ->
                    if (i < n - offset) {
                        window[i + offset]
                    } else {
                        if (iterator.hasNext()) iterator.next()
                        else ZeroSample
                    }
                }

            },
            isNotEmptyFn = { vector -> vector.all { it != ZeroSample } },
            applyFn = { (x, h) -> (h * x).sum() }
    )
}

data class ExtractNextVectorFnArgument<T : Any, L : Any>(
        val size: Int,
        val offset: Int,
        val vector: L,
        val iterator: Iterator<T>
)

class SincResampleFn<T : Any, L : Any>(initParameters: FnInitParameters) : Fn<ResamplingArgument<T>, Sequence<T>>(initParameters) {

    constructor(
            windowSize: Int,
            createVectorFn: Fn<Pair<Int, Iterator<T>>, L>,
            extractNextVectorFn: Fn<ExtractNextVectorFnArgument<T, L>, L>,
            isNotEmptyFn: Fn<L, Boolean>,
            applyFn: Fn<Pair<L, DoubleArray>, T>
    ) : this(FnInitParameters()
            .add("windowSize", windowSize)
            .add("createVectorFn", createVectorFn)
            .add("extractNextVectorFn", extractNextVectorFn)
            .add("isNotEmptyFn", isNotEmptyFn)
            .add("applyFn", applyFn)
    )

    constructor(
            windowSize: Int,
            createVectorFn: (Pair<Int, Iterator<T>>) -> L,
            extractNextVectorFn: (ExtractNextVectorFnArgument<T, L>) -> L,
            isNotEmptyFn: (L) -> Boolean,
            applyFn: (Pair<L, DoubleArray>) -> T
    ) : this(
            windowSize,
            wrap(createVectorFn),
            wrap(extractNextVectorFn),
            wrap(isNotEmptyFn),
            wrap(applyFn),
    )

    private val windowSize: Int by lazy { initParameters.int("windowSize") }
    private val createVectorFn: Fn<Pair<Int, Iterator<T>>, L> by lazy { initParameters.fn<Pair<Int, Iterator<T>>, L>("createVectorFn") }
    private val extractNextVectorFn: Fn<ExtractNextVectorFnArgument<T, L>, L> by lazy { initParameters.fn<ExtractNextVectorFnArgument<T, L>, L>("extractNextVectorFn") }
    private val isNotEmptyFn: Fn<L, Boolean> by lazy { initParameters.fn<L, Boolean>("isNotEmptyFn") }
    private val applyFn: Fn<Pair<L, DoubleArray>, T> by lazy { initParameters.fn<Pair<L, DoubleArray>, T>("applyFn") }

    private fun createVector(size: Int, iterator: Iterator<T>): L = createVectorFn.apply(Pair(size, iterator))
    private fun extractNextVector(size: Int, offset: Int, vector: L, iterator: Iterator<T>): L =
            extractNextVectorFn.apply(ExtractNextVectorFnArgument(size, offset, vector, iterator))

    private fun isNotEmpty(vector: L): Boolean = isNotEmptyFn.apply(vector)
    private fun apply(vector: L, filter: DoubleArray): T = applyFn.apply(Pair(vector, filter))

    override fun apply(argument: ResamplingArgument<T>): Sequence<T> {
        fun sinc(t: Double) = if (t == 0.0) 1.0 else sin(PI * t) / (PI * t)

        require(windowSize > 0) { "Window is too small: windowSize=$windowSize" }
        val fs = argument.inputSampleRate.toDouble()
        val nfs = argument.outputSampleRate.toDouble()

        // windowing
        val streamIterator = argument.inputSequence.iterator()
        var window: L? = null
        var windowStartIndex = 0

        fun extractWindow(on: Double): L {
            if (window == null) {
                window = createVector(windowSize, streamIterator)
            } else {
                val startIndex = windowStartIndex.toDouble()
                val offset = (on - startIndex).toInt()
                if (offset > 0) {
                    window = extractNextVector(windowSize, offset, window!!, streamIterator)
                    windowStartIndex += offset
                }
            }
            return window!!
        }

        // resampling
        fun h(t: Double, x: Double): DoubleArray {
            val p = t * fs - truncate(x * fs)
            return sampleVectorOf(
                    (-windowSize / 2 until windowSize / 2)
                            .map { it - p }
                            .map { sinc(it) }
            )
        }

        var timeMarker = 0.0
        val d = 1.0 / nfs
        return object : Iterator<T> {
            override fun hasNext(): Boolean = isNotEmpty(extractWindow(timeMarker))

            override fun next(): T {
                val sourceTimeMarker = (truncate(timeMarker * fs)) / fs // in seconds
                val x = extractWindow(sourceTimeMarker * fs)
                val h = h(timeMarker, sourceTimeMarker)
                timeMarker += d
                return apply(x, h)
            }
        }.asSequence()
    }
}

class SimpleResampleFn<T : Any>(initParameters: FnInitParameters) : Fn<ResamplingArgument<T>, Sequence<T>>(initParameters) {

    constructor(reduceFn: Fn<List<T>, T>) : this(FnInitParameters().add("reduceFn", reduceFn))

    constructor(reduceFn: (List<T>) -> T) : this(wrap(reduceFn))

    private val reduceFn: Fn<List<T>, T> by lazy { initParameters.fn<List<T>, T>("reduceFn") }

    override fun apply(argument: ResamplingArgument<T>): Sequence<T> {
        val reverseFactor = 1.0f / argument.inputOutputScaleFactor

        return if (argument.inputOutputScaleFactor == truncate(argument.inputOutputScaleFactor) || reverseFactor == truncate(reverseFactor)) {
            when {
                argument.inputOutputScaleFactor > 1 -> argument.inputSequence
                        .map { sample -> (0 until argument.inputOutputScaleFactor.toInt()).asSequence().map { sample } }
                        .flatten()
                argument.inputOutputScaleFactor < 1 -> argument.inputSequence
                        .windowed(reverseFactor.toInt(), reverseFactor.toInt(), partialWindows = true)
                        .map { samples -> reduceFn.apply(samples) }
                else -> argument.inputSequence
            }
        } else {
            throw UnsupportedOperationException("That implementation doesn't support non-integer " +
                    "input-output scale factor, but it is ${argument.inputOutputScaleFactor}")
        }
    }
}

@Serializable(with = ResampleStreamParamsSerializer::class)
class ResampleStreamParams<T>(
        val to: Float?,
        val resampleFn: Fn<ResamplingArgument<T>, Sequence<T>>
) : BeanParams()

object ResampleStreamParamsSerializer : KSerializer<ResampleStreamParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ResampleStreamParamsSerializer::class.jvmName) {
        element("to", Float.serializer().nullable.descriptor)
        element("resampleFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): ResampleStreamParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var to: Float? = null
        var resampleFn: Fn<ResamplingArgument<Any>, Sequence<Any>>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> to = dec.decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                1 -> resampleFn = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<ResamplingArgument<Any>, Sequence<Any>>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return ResampleStreamParams(to, resampleFn!!)
    }

    override fun serialize(encoder: Encoder, value: ResampleStreamParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeNullableSerializableElement(descriptor, 0, Float.serializer(), value.to)
        structure.encodeSerializableElement(descriptor, 1, FnSerializer, value.resampleFn)
        structure.endStructure(descriptor)
    }

}


class ResampleStream<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: ResampleStreamParams<T>,
) : BeanStream<T>, SingleBean<T>, SinglePartitionBean {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override val desiredSampleRate: Float? = parameters.to

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val ifs = input.desiredSampleRate ?: sampleRate
        val ofs = parameters.to ?: sampleRate
        val sequence = input.asSequence(ifs)
        val factor = ofs / ifs
        val argument = ResamplingArgument(ifs, ofs, factor, sequence)
        log.trace { "Initialized resampling from ${ifs}Hz to ${ofs}Hz [$argument]" }
        return parameters.resampleFn.apply(argument)
    }
}