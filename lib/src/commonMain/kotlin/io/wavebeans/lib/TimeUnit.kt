package io.wavebeans.lib

// Scales as constants
private const val NANO_SCALE = 1L
private const val MICRO_SCALE = 1000L * NANO_SCALE
private const val MILLI_SCALE = 1000L * MICRO_SCALE
private const val SECOND_SCALE = 1000L * MILLI_SCALE
private const val MINUTE_SCALE = 60L * SECOND_SCALE
private const val HOUR_SCALE = 60L * MINUTE_SCALE
private const val DAY_SCALE = 24L * HOUR_SCALE

/**
 * General conversion utility.
 *
 * @param d duration
 * @param dst result unit scale
 * @param src source unit scale
 */
private fun cvt(d: Long, dst: Long, src: Long): Long {
    if (src == dst) return d
    if (src < dst) return d / (dst / src)

    val r: Long = src / dst
    val m: Long = Long.MAX_VALUE / r
    return when {
        d > m -> Long.MAX_VALUE
        d < -m -> Long.MIN_VALUE
        else -> d * r
    }
}

enum class TimeUnit(
    /**
     * Instances cache conversion ratios and saturation cutoffs for
     * the units up through SECONDS. Other cases compute them, in
     * method cvt.
     */
    private val scale: Long
) {
    /**
     * Time unit representing one thousandth of a microsecond.
     */
    NANOSECONDS(NANO_SCALE),

    /**
     * Time unit representing one thousandth of a millisecond.
     */
    MICROSECONDS(MICRO_SCALE),

    /**
     * Time unit representing one thousandth of a second.
     */
    MILLISECONDS(MILLI_SCALE),

    /**
     * Time unit representing one second.
     */
    SECONDS(SECOND_SCALE),

    /**
     * Time unit representing sixty seconds.
     * @since 1.6
     */
    MINUTES(MINUTE_SCALE),

    /**
     * Time unit representing sixty minutes.
     * @since 1.6
     */
    HOURS(HOUR_SCALE),

    /**
     * Time unit representing twenty four hours.
     * @since 1.6
     */
    DAYS(DAY_SCALE);

    private val maxNanos: Long
    private val maxMicros: Long
    private val maxMillis: Long
    private val maxSecs: Long
    private val microRatio: Long
    private val milliRatio // fits in 32 bits
            : Int
    private val secRatio // fits in 32 bits
            : Int

    init {
        maxNanos = Long.MAX_VALUE / scale
        val ur = if (scale >= MICRO_SCALE) scale / MICRO_SCALE else MICRO_SCALE / scale
        microRatio = ur
        maxMicros = Long.MAX_VALUE / ur
        val mr = if (scale >= MILLI_SCALE) scale / MILLI_SCALE else MILLI_SCALE / scale
        milliRatio = mr.toInt()
        maxMillis = Long.MAX_VALUE / mr
        val sr = if (scale >= SECOND_SCALE) scale / SECOND_SCALE else SECOND_SCALE / scale
        secRatio = sr.toInt()
        maxSecs = Long.MAX_VALUE / sr
    }

    /**
     * Converts the given time duration in the given unit to this unit.
     * Conversions from finer to coarser granularities truncate, so
     * lose precision. For example, converting `999` milliseconds
     * to seconds results in `0`. Conversions from coarser to
     * finer granularities with arguments that would numerically
     * overflow saturate to `Long.MIN_VALUE` if negative or
     * `Long.MAX_VALUE` if positive.
     *
     *
     * For example, to convert 10 minutes to milliseconds, use:
     * `TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES)`
     *
     * @param sourceDuration the time duration in the given `sourceUnit`
     * @param sourceUnit the unit of the `sourceDuration` argument
     * @return the converted duration in this unit,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     */
    fun convert(sourceDuration: Long, sourceUnit: TimeUnit): Long {
        return when (this) {
            NANOSECONDS -> sourceUnit.toNanos(sourceDuration)
            MICROSECONDS -> sourceUnit.toMicros(sourceDuration)
            MILLISECONDS -> sourceUnit.toMillis(sourceDuration)
            SECONDS -> sourceUnit.toSeconds(sourceDuration)
            else -> cvt(sourceDuration, scale, sourceUnit.scale)
        }
    }

    /**
     * Equivalent to
     * [NANOSECONDS.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     */
    fun toNanos(duration: Long): Long {
        var s: Long
        var m: Long
        return if (scale.also { s = it } == NANO_SCALE) duration else if (duration > maxNanos.also {
                m = it
            }) Long.MAX_VALUE else if (duration < -m) Long.MIN_VALUE else duration * s
    }

    /**
     * Equivalent to
     * [MICROSECONDS.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     */
    fun toMicros(duration: Long): Long {
        var s: Long
        var m: Long
        return if (scale.also {
                s = it
            } <= MICRO_SCALE) if (s == MICRO_SCALE) duration else duration / microRatio else if (duration > maxMicros.also {
                m = it
            }) Long.MAX_VALUE else if (duration < -m) Long.MIN_VALUE else duration * microRatio
    }

    /**
     * Equivalent to
     * [MILLISECONDS.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     */
    fun toMillis(duration: Long): Long {
        var s: Long
        var m: Long
        return if (scale.also {
                s = it
            } <= MILLI_SCALE) if (s == MILLI_SCALE) duration else duration / milliRatio else if (duration > maxMillis.also {
                m = it
            }) Long.MAX_VALUE else if (duration < -m) Long.MIN_VALUE else duration * milliRatio
    }

    /**
     * Equivalent to
     * [SECONDS.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     */
    fun toSeconds(duration: Long): Long {
        var s: Long
        var m: Long
        return if (scale.also {
                s = it
            } <= SECOND_SCALE) if (s == SECOND_SCALE) duration else duration / secRatio else if (duration > maxSecs.also {
                m = it
            }) Long.MAX_VALUE else if (duration < -m) Long.MIN_VALUE else duration * secRatio
    }

    /**
     * Equivalent to
     * [MINUTES.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     * @since 1.6
     */
    fun toMinutes(duration: Long): Long {
        return cvt(duration, MINUTE_SCALE, scale)
    }

    /**
     * Equivalent to
     * [HOURS.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration,
     * or `Long.MIN_VALUE` if conversion would negatively overflow,
     * or `Long.MAX_VALUE` if it would positively overflow.
     * @since 1.6
     */
    fun toHours(duration: Long): Long {
        return cvt(duration, HOUR_SCALE, scale)
    }

    /**
     * Equivalent to
     * [DAYS.convert(duration, this)][.convert].
     * @param duration the duration
     * @return the converted duration
     * @since 1.6
     */
    fun toDays(duration: Long): Long {
        return cvt(duration, DAY_SCALE, scale)
    }

    /**
     * Utility to compute the excess-nanosecond argument to wait,
     * sleep, join.
     * @param d the duration
     * @param m the number of milliseconds
     * @return the number of nanoseconds
     */
    private fun excessNanos(d: Long, m: Long): Int {
        var s: Long
        return if (scale.also {
                s = it
            } == NANO_SCALE) (d - m * MILLI_SCALE).toInt() else if (s == MICRO_SCALE) (d * 1000L - m * MILLI_SCALE).toInt() else 0
    }


}
