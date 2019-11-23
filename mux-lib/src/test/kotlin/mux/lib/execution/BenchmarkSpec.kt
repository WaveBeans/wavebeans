package mux.lib.execution

import mux.lib.stream.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.system.measureTimeMillis

@ExperimentalStdlibApi
object BenchmarkSpec : Spek({
    describe("test") {
        val time = 100L//00000L
        val partitions = 2

        val i1 = seqStream()//440.sine(0.5)
        val i2 = seqStream()//800.sine(0.0)

        val p1 = i1.changeAmplitude(1.7)
        val p2 = i2.changeAmplitude(1.8)
                //.rangeProjection(0, 1000)

        val o1 = (p1 * 2.0 * 3.0 * 4.0 * 4.0 * 3.0 * 5.0 * 2.0)
                .trim(time)
                .toDevNull()
        val o2 = ((p1 * 2.0 * 3.0 * 4.0 * 4.0 * 3.0 * 5.0 * 2.0) + p2)
                .trim(time)
                .toDevNull()

        val topology = listOf(o1, o2).buildTopology()
                .partition(partitions)
                .groupBeans()
                .also { println(TopologySerializer.serialize(it)) }

        val overseer = Overseer()

        val timeToDeploy = measureTimeMillis {
            overseer.deployTopology(topology)
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

        println("Deploy took $timeToDeploy ms, processing took $timeToProcess ms, " +
                "finalizing took $timeToFinalize ms, local run time is $localRunTime ms")
    }

})