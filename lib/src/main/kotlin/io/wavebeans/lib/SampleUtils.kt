package io.wavebeans.lib

import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Converts number of samples with specified sample rate to length in specified time unit.
 * Returned values in long so it may loose precision, the highest precision in nano-seconds.
 * I.e. 50 samples with sample rate 50 Hz is 1000ms, or 1 seconds, or 0 hours.
 */
fun samplesCountToLength(samplesCount: Long, sampleRate: Float, timeUnit: TimeUnit): Long =
        (samplesCount.toDouble() /
                (sampleRate.toDouble() /
                        (1_000_000_000.0 / timeUnit.toNanos(1))
                        )
                ).toLong()

fun timeToSampleIndexFloor(timePoint: Long, timeUnit: TimeUnit, sampleRate: Float): Long =
        floor(timeUnit.toNanos(timePoint).toDouble() / 1_000_000_000.0 * sampleRate).toLong()

fun timeToSampleIndexCeil(timePoint: Long, timeUnit: TimeUnit, sampleRate: Float): Long =
        ceil(timeUnit.toNanos(timePoint).toDouble() / 1_000_000_000.0 * sampleRate).toLong()

fun timeToSampleIndexFloor(timeMarker: TimeMeasure, sampleRate: Float): Long =
        timeToSampleIndexFloor(timeMarker.time, timeMarker.timeUnit, sampleRate)

fun timeToSampleIndexCeil(timeMarker: TimeMeasure, sampleRate: Float): Long =
        timeToSampleIndexCeil(timeMarker.time, timeMarker.timeUnit, sampleRate)