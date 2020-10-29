package io.wavebeans.tests

import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.window.window
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

fun main() {
    samplesCarveOut()
}

private fun sampleArray() {
    val stream = 440.sine()
    val o = stream
            .window(128).map { sampleArrayOf(it) }
            .trim(20, TimeUnit.SECONDS)
            .toMono16bitWav("file:///users/asubb/tmp/test/sine.wav")
    val ov = SingleThreadedOverseer(listOf(o))
    ov.eval(44100.0f).all { it.get().finished }
    ov.close()
}

private fun cutOff() {
    val timeStreamMs = input { (i, sampleRate) -> i / (sampleRate / 1000.0).toLong() }
    val o = 440.sine()
            .merge(timeStreamMs) { (signal, time) ->
                checkNotNull(signal)
                checkNotNull(time)
                signal to time
            }
            // ~2ms windows with sample rate 44100Hz
            .window(89) { ZeroSample to 0 }
            .map { window ->
                val samples = window.elements.map { it.first }
                val timeMarker = window.elements.first().second
                sampleArrayOf(samples).withOutputSignal(
                        if (timeMarker > 0 && // ignore the first marker
                                timeMarker % 1000 < 2 // target only time amrker within 2 ms range
                        ) FlushOutputSignal else NoopOutputSignal,
                        ZonedDateTime.now() to timeMarker
                )
            }
            .trim(20, TimeUnit.SECONDS)
            .toMono16bitWav("file:///users/asubb/tmp/test/sine.wav") { a ->
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                "-${dtf.format(a?.first ?: ZonedDateTime.now())}-${a?.second ?: 0}"
            }
    val ov = SingleThreadedOverseer(listOf(o))
    ov.eval(44100.0f).all { it.get().finished }
    ov.close()
}

private fun samplesCarveOut() {
    val noise = input { sampleOf(Random.nextInt()) }
    val silence = (noise * 0.1).trim(100)
    val sample1 = (440.sine() + noise * 0.1).trim(500)
    val sample2 = (220.sine() + noise * 0.1).trim(500)
    val sample3 = (880.sine() + noise * 0.1).trim(500)
    val o =
            (sample1..silence..sample2..silence..sample2..sample3)
                    .window(20)
                    .map {
                        val noiseLevel = 0.11
                        val signal =
                                if (it.elements.map(::abs).average() < noiseLevel ) {
                                    CloseGateOutputSignal
                                } else {
                                    OpenGateOutputSignal
                                }

                        sampleArrayOf(it).withOutputSignal(signal, ZonedDateTime.now())
                    }
                    .toMono16bitWav("file:///users/asubb/tmp/test/sine.wav") { a ->
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                        "-${dtf.format(a ?: ZonedDateTime.now())}-${Random.nextInt(Int.MAX_VALUE).toString(36)}"
                    }

    val ov = SingleThreadedOverseer(listOf(o))
    ov.eval(44100.0f).all { it.get().finished }
    ov.close()
}