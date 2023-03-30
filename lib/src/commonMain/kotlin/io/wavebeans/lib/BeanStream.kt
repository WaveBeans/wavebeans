package io.wavebeans.lib

/**
 * Main interface for all [Bean]s that may stream data. Streams data as sequence with defined sample rate.
 * Objects it is streaming will often be referred as sample, but usually in that context it doesn't refer to object [Sample],
 * however sometimes it may be the same thing.
 *
 * @param T the type of the streaming sample.
 */
interface BeanStream<T : Any> : Bean<T> {

    /**
     *  Gets the input as a sequence of samples.
     *
     *  @param sampleRate sample rate to use for generating sequence. If sample rate is different
     *          from the source iy should be resampled, or an exception should be thrown if it's not supported
     *  @throws UnsupportedOperationException if input doesn't support resampling
     **/
    fun asSequence(sampleRate: Float): Sequence<T>

    /**
     * Counts number of occurrences of [T] objects in the sequence.
     * **Caution: it reads the whole stream, do not expect execution to end on infinite streams**
     */
    fun samplesCount(sampleRate: Float): Long = asSequence(sampleRate).count().toLong()

    /**
     * Measures the length in the sequence
     * **Caution: it reads the whole stream, do not expect execution to end on infinite streams**
     */
    fun length(sampleRate: Float, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long = samplesCountToLength(samplesCount(sampleRate), sampleRate, timeUnit)

    /**
     * Defines the sample rate the bean desires to stream in, or `null` if it doesn't matter, it can work with any.
     *
     *
     * Most of the operations, like `map()` would just by pass the input value, whereas input operations would define
     * it strictly, i.e. if samples are read from file and should be interpreted with the certain sample
     * rate (i.e. wave input).
     *
     * Though there are operations like `resample()` which adopt their input and output beans sample rates, in this case
     * it still reads the sample rate from the input, but returns desired sample rate as requested or as configured.
     * Whilst doing the resample magic.
     */
    val desiredSampleRate: Float?
}
