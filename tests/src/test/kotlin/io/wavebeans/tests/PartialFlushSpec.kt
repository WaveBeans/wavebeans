package io.wavebeans.tests

import assertk.all
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.fail
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.merge
import io.wavebeans.lib.stream.rangeTo
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.window
import io.wavebeans.metrics.*
import io.wavebeans.metrics.collector.MetricCollector
import io.wavebeans.metrics.collector.collector
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

object PartialFlushSpec : Spek({

    val outputDir by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
    fun outputFiles() = outputDir.listFiles()?.toList()?.sorted() ?: emptyList<File>()
    val ports = arrayOf(Random.nextInt(10000, 50000), Random.nextInt(10000, 50000))
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
            Param("distributed", { facilitatorLocations }) { o, sampleRate, facilitatorLocations ->
                o.evaluateInDistributedMode(sampleRate, facilitatorLocations)
            },
    )

    describe("WAV") {
        describe("cut into 100 millisecond pieces") {
            modes.forEach { (mode, locateFacilitators, evaluate) ->
                it("should perform in $mode mode") {
                    val flushCounter = flushedOnOutputMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 1000,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val gateState = gateStateOnOutputMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 1000,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val outputState = outputStateMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 1000,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val inputProcessed = samplesProcessedOnInputMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 1000,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()

                    val sampleRate = 5000.0f
                    val timeStreamMs = input { (i, sampleRate) -> i / (sampleRate / 1000.0).toLong() }
                    val input = 440.sine()
                    val o = input
                            .merge(timeStreamMs) { (signal, time) ->
                                checkNotNull(signal)
                                checkNotNull(time)
                                signal to time
                            }
                            .trim(2, TimeUnit.SECONDS)
                            // 2ms windows within desired sample rate 5000 Hz
                            .window(10) { ZeroSample to 0 }
                            .map { window ->
                                val samples = window.elements.map { it.first }
                                val timeMarker = window.elements.first().second
                                sampleArrayOf(samples).withOutputSignal(
                                        if (timeMarker > 0 // ignore the first marker to avoid flushing empty file
                                                && timeMarker % 100 < 2 // target every 100 millisecond notch with 2 ms precision
                                        ) FlushOutputSignal else NoopOutputSignal,
                                        timeMarker
                                )
                            }
                            .toMono16bitWav("file://${outputDir.absolutePath}/sine.wav") {
                                "-${((it ?: 0) / 100).toString().padStart(2, '0')}"
                            }
                    evaluate(o, sampleRate, locateFacilitators())

                    flushCounter.collectFromDownstream(Long.MAX_VALUE)
                    assertThat(flushCounter.collectValues(Long.MAX_VALUE))
                            .prop("flushesCount") { v -> v.map { it.value }.sum() }
                            .isCloseTo(19.0, 1e-16)

                    gateState.collectFromDownstream(Long.MAX_VALUE)
                    assertThat(gateState.collectValues(Long.MAX_VALUE))
                            .prop("lastReport") { it.last().value.increment }
                            .isEqualTo(1.0)

                    outputState.collectFromDownstream(Long.MAX_VALUE)
                    assertThat(outputState.collectValues(Long.MAX_VALUE))
                            .prop("lastReport") { it.last().value.increment }
                            .isEqualTo(0.0)

                    inputProcessed.collectFromDownstream(Long.MAX_VALUE)
                    val expectedSampleGenerated = sampleRate * 2.0 /*sec*/ * 2.0 /*inputs*/
                    assertThat(inputProcessed.collectValues(Long.MAX_VALUE))
                            .prop("values.sum()") { it.map { it.value }.sum() }
                            .isCloseTo(expectedSampleGenerated, expectedSampleGenerated * 0.05)

                    inputProcessed.close()
                    flushCounter.close()
                    gateState.close()
                    outputState.close()

                    assertThat(outputFiles()).eachIndexed(20) { file, index ->
                        val suffix = index.toString().padStart(2, '0')
                        val samples = input.asSequence(sampleRate).drop(index * sampleRate.toInt()).take(sampleRate.toInt()).toList()
                        file.prop("name") { it.name }.isEqualTo("sine-$suffix.wav")
                        val expectedSize = 44 + sampleRate * BitDepth.BIT_16.bytesPerSample * 2.0 /*sec*/ / 20.0 /*files*/
                        file.prop("size") { it.readBytes().size.toDouble() }.isCloseTo(expectedSize, expectedSize * 0.05)
                        file.prop("content") {
                            wave("file://${it.absolutePath}").asSequence(sampleRate).toList()
                        }.isContainedBy(samples) { a, b -> abs(a - b) < 1e-4 }
                    }

                }
            }
        }

        describe("Store samples of the stream separately") {
            modes.forEach { (mode, locateFacilitators, evaluate) ->
                it("should perform in $mode mode") {
                    val gateState = gateStateOnOutputMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 0,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val outputState = outputStateMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 1000,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val inputProcessed = samplesProcessedOnInputMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 10,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()

                    val sampleRate = 500.0f
                    val silence1 = input { ZeroSample }.trim(100)
                    val silence2 = input { ZeroSample }.trim(200)
                    val sample1 = 40.sine().trim(500)
                    val sample2 = 20.sine().trim(500)
                    val sample3 = 80.sine().trim(500)

                    val windowSize = 10
                    val o = (sample1..silence1..sample2..silence2..sample3)
                            .window(windowSize)
                            .merge(input { it.first }.trim(1800L / windowSize)) { (window, index) ->
                                checkNotNull(index)
                                window to index
                            }
                            .map {
                                val noiseLevel = 1e-2
                                val signal = if (it.first?.elements?.map(::abs)?.average() ?: 0.0 < noiseLevel) {
                                    CloseGateOutputSignal
                                } else {
                                    OpenGateOutputSignal
                                }

                                it.first?.let { w -> sampleArrayOf(w).withOutputSignal(signal, it.second) }
                                        ?: sampleArrayOf(emptyList()).withOutputSignal(CloseOutputSignal)
                            }
                            .toMono16bitWav("file://${outputDir.absolutePath}/sine.wav") { "-${(it ?: 0).toString().padStart(2, '0')}" }
                    evaluate(o, sampleRate, locateFacilitators())

                    gateState.collectFromDownstream(Long.MAX_VALUE)
                    assertThat(gateState.collectValues(Long.MAX_VALUE), "Gate history").all {
                        prop("openedReported") { it.count { it.value.increment == 1.0 } }.isEqualTo(3)
                        prop("closedReported") { it.count { it.value.increment == -1.0 } }.isEqualTo(2)
                        prop("lastReported") { it.last().value.increment }.isEqualTo(1.0)
                    }

                    outputState.collectFromDownstream(Long.MAX_VALUE)
                    assertThat(outputState.collectValues(Long.MAX_VALUE), "Output is closed")
                            .prop("lastReport") { it.last().value.increment }
                            .isEqualTo(0.0)

                    inputProcessed.collectFromDownstream(Long.MAX_VALUE)
                    val expectedSampleGenerated = sampleRate * (0.1 + 0.2 + 0.5 + 0.5 + 0.5) /*sec*/ * 1.1 /*inputs + windowed indexer*/
                    assertThat(inputProcessed.collectValues(Long.MAX_VALUE), "All inputs read")
                            .prop("values.sum()") { it.map { it.value }.sum() }
                            .isCloseTo(expectedSampleGenerated, expectedSampleGenerated * 0.05)

                    inputProcessed.close()
                    gateState.close()
                    outputState.close()

                    assertThat(outputFiles()).eachIndexed(3) { file, index ->
                        val (suffix, samples) = when (index) {
                            0 -> "00" to sample1
                            1 -> "30" to sample2
                            2 -> "65" to sample3
                            else -> throw UnsupportedOperationException("$index is not supported")
                        }
                        file.prop("name") { it.name }.isEqualTo("sine-$suffix.wav")
                        file.prop("content") {
                            wave("file://${it.absolutePath}").asSequence(sampleRate).toList()
                        }.isContainedBy(samples.asSequence(sampleRate).toList()) { a, b -> abs(a - b) < 1e-4 }

                    }
                }
            }
        }

        describe("End the stream on specific sample sequence") {
            class SequenceDetectFn(initParameters: FnInitParameters)
                : Fn<Window<Sample>, Managed<OutputSignal, Unit, SampleArray>>(initParameters) {

                constructor(endSequence: List<Sample>) : this(FnInitParameters().addDoubles("endSequence", endSequence))

                override fun apply(argument: Window<Sample>): Managed<OutputSignal, Unit, SampleArray> {
                    val es = initParams.doubles("endSequence")
                    val ei = argument.elements.iterator()
                    var ai = es.iterator()
                    var startedAt = -1
                    var i = 0
                    while (ei.hasNext() && ai.hasNext()) {
                        val e = ei.next()
                        val a = ai.next()
                        if (a != e) {
                            ai = es.iterator()
                            startedAt = -1
                        } else if (startedAt == -1) {
                            startedAt = i
                        }
                        i++
                    }

                    if (ai.hasNext()) startedAt = -1

                    return if (startedAt == -1) {
                        sampleArrayOf(argument).withOutputSignal(NoopOutputSignal)
                    } else {
                        sampleArrayOf(argument.elements.subList(0, startedAt)).withOutputSignal(CloseOutputSignal)
                    }
                }
            }

            modes.forEach { (mode, locateFacilitators, evaluate) ->
                it("should perform in $mode mode") {
                    val outputState = outputStateMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 1000,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val inputProcessed = samplesProcessedOnInputMetric.collector(
                            refreshIntervalMs = 0,
                            granularValueInMs = 10,
                            downstreamCollectors = locateFacilitators()
                    ).attachAndRegister()
                    val sampleRate = 500.0f
                    val endSequence = listOf(1.5, 1.5, 1.5)
                    val endSignal = endSequence.input()
                    val signal = 80.sine().trim(1000)
                    val noise = input { sampleOf(Random.nextInt()) }.trim(1000)

                    val o = (signal..endSignal..noise)
                            .window(64)
                            .map(SequenceDetectFn(endSequence))
                            .toMono16bitWav("file://${outputDir.absolutePath}/sine.wav") { "-${Random.nextInt().toString(36)}" }
                    evaluate(o, sampleRate, locateFacilitators())

                    outputState.collectFromDownstream(Long.MAX_VALUE)
                    assertThat(outputState.collectValues(Long.MAX_VALUE), "Output is closed")
                            .prop("lastReport") { it.last().value.increment }
                            .isEqualTo(0.0)

                    if (mode == "local") {
                        inputProcessed.collectFromDownstream(Long.MAX_VALUE)
                        val expectedSampleGenerated = sampleRate * 1.0 /*sec*/ * 1.0 /*signal*/
                        assertThat(inputProcessed.collectValues(Long.MAX_VALUE), "All inputs read")
                                .prop("values.sum()") { it.map { it.value }.sum() }
                                .isCloseTo(expectedSampleGenerated, expectedSampleGenerated * 0.05)
                    } else {
                        // non-local processor are greedy, and read up to a noise
                        inputProcessed.collectFromDownstream(Long.MAX_VALUE)
                        val expectedSampleGenerated = sampleRate * 1.0 /*sec*/ * 2.0 /*signal + noise*/
                        assertThat(inputProcessed.collectValues(Long.MAX_VALUE), "All inputs read")
                                .prop("values.sum()") { it.map { it.value }.sum() }
                                .isCloseTo(expectedSampleGenerated, expectedSampleGenerated * 0.05)
                    }

                    inputProcessed.close()
                    outputState.close()

                    assertThat(outputFiles()).eachIndexed(1) { file, _ ->
                        val samples = signal.asSequence(sampleRate).toList()
                        file.prop("content") {
                            wave("file://${it.absolutePath}").asSequence(sampleRate).toList()
                        }.isContainedBy(samples) { a, b -> abs(a - b) < 1e-4 }

                    }
                }
            }
        }
    }
})

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
