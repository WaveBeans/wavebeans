package mux.cli.command

import mux.cli.Session
import mux.lib.execution.*
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.changeAmplitude
import mux.lib.stream.plus
import mux.lib.stream.trim
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

            val i1 = 440.sine(0.5)
            val i2 = 800.sine(0.0)

            val p1 = i1.changeAmplitude(1.7)
            val p2 = i2.changeAmplitude(1.8)
                    .rangeProjection(0, 1000)

            val o1 = p1
                    .trim(time)
                    .toCsv("file://${f1.absolutePath}")
            val o2 = (p1 + p2)
                    .trim(time)
                    .toCsv("file://${f2.absolutePath}")

            val topology = listOf(o1, o2).buildTopology()
                    .partition(partitions)
                    .groupBeans()

            val overseer = Overseer()

            val timeToDeploy = measureTimeMillis {
                overseer.deployTopology(topology, threadsPerBush)
            }
            val timeToProcess = measureTimeMillis {
                overseer.waitToFinish(1)
            }
            val timeToFinalize = measureTimeMillis {
                overseer.close()
            }

            val localRunTime = measureTimeMillis {
                listOf(o1, o2)
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
