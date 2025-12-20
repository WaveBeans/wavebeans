package io.wavebeans.execution.distributed

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.assertions.support.fail
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.execution.eachIndexed
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.*
import io.wavebeans.lib.plus
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.window.hamming
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

class DistributedOverseerSpec : DescribeSpec({

    val log = KotlinLogging.logger {}
    val ports = createPorts(2)
    val facilitatorsLocations by lazy { listOf("127.0.0.1:${ports[0]}", "127.0.0.1:${ports[1]}") }

    val pool by lazy { Executors.newCachedThreadPool() }

    beforeSpec {
        pool.submit { startFacilitator(ports[0]) }
        pool.submit { startFacilitator(ports[1]) }

        facilitatorsLocations.forEach(::waitForFacilitatorToStart)
    }

    afterSpec {
        try {
            facilitatorsLocations.forEach(::terminateFacilitator)
        } finally {
            pool.shutdownNow()
        }
    }


    describe("Running code accessible by Facilitator without extra loading") {

        fun assertExecution(outputs: () -> List<Pair<StreamOutput<out Any>, File>>) {
            val outputsDistributed by lazy { outputs() }
            val distributed by lazy {
                DistributedOverseer(
                    outputsDistributed.map { it.first },
                    facilitatorsLocations,
                    emptyList(),
                    2
                )
            }

            val outputsSingle by lazy { outputs() }
            val single by lazy {
                SingleThreadedOverseer(outputsSingle.map { it.first })
            }

            //it("shouldn't throw any exceptions") {
            val exceptions = distributed.use {
                it.eval(44100.0f)
                    .mapNotNull { it.get().exception }
            }
            assertThat(exceptions).isEmpty()

            //it("shouldn't throw any exceptions") {
            val exceptions2 = single.use {
                it.eval(44100.0f)
                    .mapNotNull { it.get().exception }
            }
            assertThat(exceptions2).isEmpty()

            //it("should have the same output") {
            assertThat(outputsDistributed).eachIndexed { o, i ->
                o.prop("fileContent") { it.second.readLines() }.all {
                    size().isGreaterThan(1)
                    isEqualTo(outputsSingle[i].second.readLines())
                }
            }
        }

        it("should execute using builtin functions and types") {
            val outputs = {
                val file1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val file2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val input = 440.sine().map { abs(it) } + 880.sine()
                val output1 = input
                    .trim(500)
                    .toCsv("file:///${file1.absolutePath}")
                val output2 = input.trim(1000)
                    .window(101, 25)
                    .hamming()
                    .fft(128)
                    .trim(500)
                    .magnitudeToCsv("file:///${file2.absolutePath}")
                listOf(output1 to file1/*, output2 to file2*/)
            }

            assertExecution(outputs)
        }

        it("should execute using custom types") {

            @Serializable
            data class InnerSample(val v: Sample) : Measured {
                override fun measure(): Int = SampleCountMeasurement.samplesInObject(v)
            }

            @Serializable
            data class MySample(val v: InnerSample) : Measured {
                override fun measure(): Int = SampleCountMeasurement.samplesInObject(v)
            }

            val outputs = {

                val file1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val input = 440.sine().map { MySample(InnerSample(abs(it))) }
                    .merge(
                        with = 880.sine()
                            .map { MySample(InnerSample(it)) }) { (a, b) -> MySample(InnerSample(a?.v?.v + b?.v?.v)) }
                val output1 = input
                    .trim(500)
                    .toCsv(
                        uri = "file:///${file1.absolutePath}",
                        header = listOf("index", "value"),
                        elementSerializer = { (i, _, v) ->
                            listOf(i.toString(), v.v.v.toString())
                        }
                    )
                listOf(output1 to file1)
            }

            assertExecution(outputs)
        }
    }

    describe("Failing on error") {

        fun assertExecutionExceptions(
            outputs: () -> List<StreamOutput<out Any>>,
            assertBlock: Assert<List<Throwable>>.() -> Unit
        ) {
            val outputsDistributed by lazy { outputs() }
            val distributed by lazy {
                DistributedOverseer(
                    outputsDistributed,
                    facilitatorsLocations,
                    emptyList(),
                    2
                )
            }

            val exceptions = distributed.eval(44100.0f)
                .mapNotNull { it.get().exception }
            distributed.close()
            assertBlock(assertThat(exceptions))
        }

        it("it should execute when used non serializable class") {

            data class NonSerializable(val v: Sample)

            val outputs = {
                val input = 440.sine().map { NonSerializable(it) }
                val output1 = input
                    .trim(500)
                    .toDevNull()
                listOf(output1)
            }

            assertExecutionExceptions(outputs) {
                isNotEmpty()
                size().isEqualTo(1)
                prop("first") { it.first() }.message().isNotNull().contains("NonSerializable")
            }
        }

        it("should execute when exception happened during execution of some bean") {
            val outputs = {
                val input = 440.sine().map { check(false) { "Doesn't work" }; it }
                val output1 = input
                    .trim(500)
                    .toDevNull()
                listOf(output1)
            }

            assertExecutionExceptions(outputs) {
                isNotEmpty()
                size().isEqualTo(1)
                prop("first") { it.first() }
                    .cause().isNotNull()
                    .message().isNotNull().contains("Doesn't work")
            }
        }
    }

    describe("Running code not accessible by Facilitator") {
        val file = File.createTempFile("testAppOut", ".csv")

        fun codeFile(name: String): Pair<String, String> {
            return name to this::class.java.getResourceAsStream("/testApp/$name")!!.reader().readText()
                .replace(
                    "/[*]FILE[*]/.*/[*]FILE[*]/".toRegex(),
                    "File(\"${file.absolutePath}\")"
                )
                .replace(
                    "/[*]FACILITATOR_LIST[*]/.*/[*]FACILITATOR_LIST[*]/".toRegex(),
                    "listOf(\"127.0.0.1:${ports[0]}\", \"127.0.0.1:${ports[1]}\")"
                )
                .also { log.info { "$name:\n$it" } }
        }

        fun extractExceptions(runCall: CommandResult): List<String> {
            return String(runCall.output)
                .replace(
                    ".*>>>> EXCEPTIONS(.*)<<<< EXCEPTIONS.*".toRegex(RegexOption.DOT_MATCHES_ALL),
                    "$1"
                )
                .trim()
                .split("----SPLITTER----")
                .filter { it.isNotEmpty() }
        }

        val jarFile by lazy {
            compileCode(
                mapOf(
                    codeFile("Runner.kt"),
                    codeFile("Success.kt"),
                    codeFile("Error.kt"),
                    codeFile("CustomType.kt")
                )
            )
        }

        describe("Successful runner") {
            val runner by lazy {
                CommandRunner(
                    javaCmd(),
                    "-cp", System.getProperty("java.class.path") + ":" + jarFile.absolutePath,
                    "io.wavebeans.test.app.SuccessKt"
                )
            }

            lateinit var runCall: CommandResult
            it("should execute") {
                runCall = runner.run(inheritIO = false)
                assertThat(runCall.exitCode).isEqualTo(0)
            }
            it("shouldn't throw any exceptions to output") {
                val exceptions = extractExceptions(runCall)
                assertThat(exceptions).isEmpty()
            }
            it("should contain 11 rows in output file") {
                assertThat(file.readLines()).all {
                    eachIndexed(11) { r, idx ->
                        when (idx) {
                            0 -> r.isEqualTo("index,value")
                            in 1..10 -> r.isEqualTo("${idx - 1},${idx - 1}")
                            else -> r.fail("index < 11", "index=$idx")
                        }
                    }
                }
            }
        }

        describe("Custom type runner") {
            val runner by lazy {
                CommandRunner(
                    javaCmd(),
                    "-cp", System.getProperty("java.class.path") + ":" + jarFile.absolutePath,
                    "io.wavebeans.test.app.CustomTypeKt"
                )
            }

            val runCall by lazy { runner.run(inheritIO = false) }
            it("should execute") {
                assertThat(runCall.exitCode).isEqualTo(0)
            }
            it("shouldn't throw any exceptions to output") {
                val exceptions = extractExceptions(runCall)
                assertThat(exceptions).isEmpty()
            }
            it("should contain 11 rows in output file") {
                assertThat(file.readLines()).all {
                    eachIndexed(11) { r, idx ->
                        when (idx) {
                            0 -> r.isEqualTo("index,value")
                            in 1..10 -> r.isEqualTo("${idx - 1},${idx - 1}")
                            else -> r.fail("index < 11", "index=$idx")
                        }
                    }
                }
            }
        }

        describe("Error runner") {
            val runner by lazy {
                CommandRunner(
                    javaCmd(),
                    "-cp", System.getProperty("java.class.path") + ":" + jarFile.absolutePath,
                    "io.wavebeans.test.app.ErrorKt"
                )
            }

            lateinit var runCall: CommandResult
            it("should execute") {
                runCall = runner.run(inheritIO = false)
                assertThat(runCall.exitCode).isEqualTo(0)
            }
            it("should throw two exceptions to output") {
                val exceptions = extractExceptions(runCall)
                assertThat(exceptions).size().isEqualTo(2)
            }
        }
    }
})


