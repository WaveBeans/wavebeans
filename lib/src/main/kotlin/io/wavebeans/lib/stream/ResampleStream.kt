package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import mu.KotlinLogging
import kotlin.math.truncate

@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        reduceFn: (List<Sample>) -> Sample = { it.average() },
        to: Float? = null,
        resampleFn: (ResamplingArgument<Sample>) -> Sequence<Sample> = ::resampleFn
): BeanStream<Sample> {
    return this.resample(Fn.wrap(reduceFn), to, Fn.wrap(resampleFn))
}

@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        reduceFn: Fn<List<Sample>, Sample>,
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>> = Fn.wrap(::resampleFn)
): BeanStream<Sample> {
    return ResampleStream(this, ResampleStreamParams(to, reduceFn, resampleFn))
}

fun <T : Any> BeanStream<T>.resample(
        reduceFn: (List<T>) -> T = { throw IllegalStateException("reduce function is not defined") },
        to: Float? = null,
        resampleFn: (ResamplingArgument<T>) -> Sequence<T> = ::resampleFn
): BeanStream<T> {
    return this.resample(Fn.wrap(reduceFn), to, Fn.wrap(resampleFn))
}

fun <T : Any> BeanStream<T>.resample(
        reduceFn: Fn<List<T>, T>,
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<T>, Sequence<T>> = Fn.wrap(::resampleFn)
): BeanStream<T> {
    return ResampleStream(this, ResampleStreamParams(to, reduceFn, resampleFn))
}

data class ResamplingArgument<T>(
        val inputSampleRate: Float,
        val outputSampleRate: Float,
        val factor: Float,
        val inputSequence: Sequence<T>,
        val reduceFn: Fn<List<T>, T>
)

fun <T : Any> resampleFn(argument: ResamplingArgument<T>): Sequence<T> {
    val reverseFactor = 1.0f / argument.factor

    return if (argument.factor == truncate(argument.factor) || reverseFactor == truncate(reverseFactor)) {
        when {
            argument.factor > 1 -> argument.inputSequence
                    .map { sample -> (0 until argument.factor.toInt()).asSequence().map { sample } }
                    .flatten()
            argument.factor < 1 -> argument.inputSequence
                    .windowed(reverseFactor.toInt(), reverseFactor.toInt(), partialWindows = true)
                    .map { samples -> argument.reduceFn.apply(samples) }
            else -> argument.inputSequence
        }
    } else {
        TODO()
    }
}

class ResampleStreamParams<T>(
        val to: Float?,
        val reduceFn: Fn<List<T>, T>,
        val resampleFn: Fn<ResamplingArgument<T>, Sequence<T>>
) : BeanParams()

class ResampleStream<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: ResampleStreamParams<T>,
) : BeanStream<T>, SingleBean<T> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override var outputSampleRate: Float? = null
        private set

    override var inputSampleRate: Float? = null
        private set

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val sequence = input.asSequence(sampleRate)
        this.inputSampleRate = input.outputSampleRate
        this.outputSampleRate = parameters.to ?: sampleRate

        val ifs = checkNotNull(inputSampleRate) { "Input doesn't provide the sample rate, resampling is not possible." }
        val ofs = outputSampleRate!!

        val factor = ofs / ifs
        val argument = ResamplingArgument(ifs, ofs, factor, sequence, parameters.reduceFn)
        log.trace { "Initialized resampling from ${ifs}Hz to ${ofs}Hz [$argument]" }
        return parameters.resampleFn.apply(argument)
    }
}