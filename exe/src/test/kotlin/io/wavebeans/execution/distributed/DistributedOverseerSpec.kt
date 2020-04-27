package io.wavebeans.execution.distributed

import assertk.assertThat
import assertk.assertions.isNull
import assertk.catch
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toDevNull
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.spekframework.spek2.style.specification.xdescribe
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.abs

object DistributedOverseerSpec : Spek({

    val pool = Executors.newCachedThreadPool()

    afterGroup {
        pool.shutdownNow()
    }

    xdescribe("111") {



//        val gardener1 = CrewGardener("127.0.0.1", 40000..50000, 10, 1)
//        val gardener2 = CrewGardener("127.0.0.1", 40000..50000, 10, 1)
//
//        Thread { gardener1.start(waitAndClose = false) }.run()
//        Thread { gardener2.start(waitAndClose = false) }.run()

//        val gardeners = mutableListOf<Future<Unit>>()
//        gardeners += pool.submit(Callable { startCrewGardener(40001) })
//        gardeners += pool.submit(Callable { startCrewGardener(40002) })
//
//        Thread.sleep(10000) // wait for the start
//
//        val input = 440.sine()
//                .map { abs(it) } + 880.sine()
//
//        val output1 = input.trim(5000).toDevNull()
//        val output2 = input.window(401, 128).fft(512).trim(1000).toDevNull()
//
//        val overseer = DistributedOverseer(
//                listOf(output1, output2),
//                listOf(
//                        "http://127.0.0.1:40001",
//                        "http://127.0.0.1:40002"
//                ),
//                2
//        )
//
//        overseer.eval(44100.0f)
//
//        it("shouldn't throw any exceptions") {
//            assertThat(catch { gardeners.forEach { it.get() } }).isNull()
//        }
    }
})

fun startCrewGardener(port: Int) {
    val confFile = File.createTempFile("crew-gardener-config", ".conf")
    confFile.writeText("""
        crewGardenderConfig {
            advertisingHostAddress: 127.0.0.1
            listeningPortRange: {start: $port, end: $port}
            threadsNumber: 1
        }
    """.trimIndent())

    val java = "java"
    val javaHome = System.getenv("JAVA_HOME")
            ?.takeIf { File("$it/$java").exists() }
            ?: System.getenv("PATH")
                    .split(":")
                    .firstOrNull { File("$it/$java").exists() }
            ?: throw IllegalStateException("$java is not located, make sure it is available via either" +
                    " PATH or JAVA_HOME environment variable")


    val runner = CommandRunner(
            "$javaHome/$java",
            "-cp", System.getProperty("java.class.path"),
            "io.wavebeans.execution.distributed.CrewGardenerKt", confFile.absolutePath
    )
    val runCall = runner.call()

    if (runCall.exitCode != 0) {
        throw IllegalStateException("Can't compile the script. \n${String(runCall.output)}")
    }

}