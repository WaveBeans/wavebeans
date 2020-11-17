package io.wavebeans.tests

import assertk.assertThat
import assertk.fail
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toMono16bitWav
import io.wavebeans.lib.io.wave
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.resample
import io.wavebeans.lib.stream.times
import io.wavebeans.lib.stream.trim
import io.wavebeans.metrics.MetricService
import io.wavebeans.metrics.collector.MetricCollector
import io.wavebeans.metrics.collector.TimedValue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import kotlin.math.abs

object ResampleSpec : Spek({

    val ports = createPorts(2)
    val facilitatorLocations = listOf("localhost:${ports[0]}", "localhost:${ports[1]}")

    beforeEachTest {
        MetricService.reset()
    }

    beforeGroup {
        Thread { startFacilitator(ports[0]) }.start()
        Thread { startFacilitator(ports[1]) }.start()
        waitForFacilitatorToStart("localhost:${ports[0]}")
        waitForFacilitatorToStart("localhost:${ports[1]}")
    }

    afterGroup {
        terminateFacilitator("localhost:${ports[0]}")
        terminateFacilitator("localhost:${ports[1]}")
    }

    data class Param(
            val mode: String,
            val locateFacilitators: () -> List<String>,
            val evaluate: (StreamOutput<*>, Float, List<String>) -> Unit
    )

    val modes: List<Param> = listOf(
            Param("local", { emptyList() }) { o, sampleRate, _ ->
                o.evaluate(sampleRate)
            },
            Param("multi-threaded", { emptyList() }) { o, sampleRate, _ ->
                o.evaluateInMultiThreadedMode(sampleRate)
            },
            Param("distributed", { facilitatorLocations }) { o, sampleRate, facilitators ->
                o.evaluateInDistributedMode(sampleRate, facilitators)
            },
    )

    describe("resampling wav file") {

        val input by memoized {
            (440.sine() * 0.2).trim(100)
        }

        val wavFile by memoized {
            val f = File.createTempFile("source", ".wav")
            input.toMono16bitWav("file://${f.absolutePath}").evaluate(11025.0f)
            wave("file://${f.absolutePath}")
        }

        val outputFile by memoized(TEST) { File.createTempFile("resample", ".wav") }

        modes.forEach { (mode, locateFacilitators, evaluate) ->
            it("should perform in $mode mode") {
                val stream = wavFile.resample(to = 44100.0f)
                        .map { it * 2.0 }
                        .resample()
                        .toMono16bitWav("file://${outputFile.absolutePath}")

                evaluate(stream, 22050.0f, locateFacilitators())

                val samples = (input * 2).toList(22050.0f)
                assertThat(wave("file://${outputFile.absolutePath}").toList(22050.0f))
                        .isContainedBy(samples) {a, b -> abs(a - b) < 0.1}

            }
        }
    }
})

private fun <T : Any> MetricCollector<T>.collect(): List<TimedValue<T>> {
    this.collectFromDownstream(Long.MAX_VALUE)
    val ret = this.collectValues(Long.MAX_VALUE)
    this.close()
    return ret
}

private fun <T : Any> MetricCollector<T>.attachAndRegister(): MetricCollector<T> {
    if (this.downstreamCollectors.isNotEmpty()) {
        val startAt = System.currentTimeMillis()
        while (!this.attachCollector()) {
            if (System.currentTimeMillis() - startAt > 5000) fail("Can't attach to downstream collector [$this]")
            sleep(0)
        }
    }
    MetricService.registerConnector(this)
    return this
}
