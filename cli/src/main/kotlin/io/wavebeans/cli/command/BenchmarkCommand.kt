package io.wavebeans.cli.command

import io.wavebeans.cli.Session
import io.wavebeans.execution.Overseer
import io.wavebeans.execution.buildTopology
import io.wavebeans.execution.groupBeans
import io.wavebeans.execution.partition
import io.wavebeans.lib.io.magnitudeToCsv
import io.wavebeans.lib.io.phaseToCsv
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.changeAmplitude
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.fft.trim
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import java.io.File
import kotlin.system.measureTimeMillis

@ExperimentalStdlibApi
class BenchmarkCommand(session: Session) : InScopeCommand(
        name = "benchmark",
        description = "Running benchmark",
        scopeModifier = { _, s ->
            val args = s?.split(" ")
                    ?: throw IllegalArgumentException("Provide time in milliseconds and partitions as parameters")
            val time = args[0].toLong()
            val partitions = args[1].toInt()
            val threadsPerBush = args[2].toInt()

            val f1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val f2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val f3 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val f4 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

            val i1 = 440.sine(0.5)
            val i2 = 800.sine(0.0)

            val p1 = i1.changeAmplitude(1.7)
            val p2 = i2.changeAmplitude(1.8)
                    .rangeProjection(0, 1000)
            val pp = p1 + p2

            val fft = pp
                    .window(401)
                    .fft(512)
                    .trim(50000)

            val o1 = p1
                    .trim(time)
                    .toCsv("file://${f1.absolutePath}")
            val o2 = pp
                    .trim(time)
                    .toCsv("file://${f2.absolutePath}")
            val o3 = fft.magnitudeToCsv("file://${f3.absolutePath}")
            val o4 = fft.phaseToCsv("file://${f4.absolutePath}")

            val topology = listOf(o1, o2, o3, o4).buildTopology()
                    .partition(partitions)
                    .groupBeans()

            val overseer = Overseer()

            val timeToDeploy = measureTimeMillis {
                overseer.deployTopology(topology, threadsPerBush)
            }
            val timeToProcess = measureTimeMillis {
                overseer.waitToFinish()
            }
            val timeToFinalize = measureTimeMillis {
                overseer.close()
            }

            val localRunTime = measureTimeMillis {
                listOf(o1, o2, o3, o4)
                        .map { it.writer(44100.0f) }
                        .forEach {
                            while (it.write()) {
                                Thread.sleep(0)
                            }
                            it.close()
                        }
            }

            "Deploy took $timeToDeploy ms, processing took $timeToProcess ms, " +
                    "finalizing took $timeToFinalize ms, local run time is $localRunTime ms"

        }
)
