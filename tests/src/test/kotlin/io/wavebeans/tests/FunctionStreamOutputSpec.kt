package io.wavebeans.tests

import assertk.assertThat
import assertk.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.wavebeans.lib.io.WbFileDriver
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.io.WriteFunctionPhase.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.times
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.metrics.MetricService
import java.io.File
import kotlin.math.abs

class FunctionStreamOutputSpec : DescribeSpec({

    val ports = createPorts(2)
    val facilitatorLocations = listOf("localhost:${ports[0]}", "localhost:${ports[1]}")

    beforeTest {
        MetricService.reset()
    }

    beforeSpec {
        Thread { startFacilitator(ports[0]) }.start()
        Thread { startFacilitator(ports[1]) }.start()
        waitForFacilitatorToStart("localhost:${ports[0]}")
        waitForFacilitatorToStart("localhost:${ports[1]}")
    }

    afterSpec {
        terminateFacilitator("localhost:${ports[0]}")
        terminateFacilitator("localhost:${ports[1]}")
    }

    data class Param(
        val mode: String,
        val locateFacilitators: () -> List<String>,
        val evaluate: (StreamOutput<*>, Float, List<String>) -> Unit
    )

    val modes = mapOf(
        "local" to Param("local", { emptyList() }) { o, sampleRate, _ ->
            o.evaluate(sampleRate)
        },
        "multi-threaded" to Param("multi-threaded", { emptyList() }) { o, sampleRate, _ ->
            o.evaluateInMultiThreadedMode(sampleRate)
        },
        "distributed" to Param("distributed", { facilitatorLocations }) { o, sampleRate, facilitators ->
            o.evaluateInDistributedMode(sampleRate, facilitators)
        },
    )

    describe("Writing encoded samples") {

        class FileEncoderFn<T : Any>(private val filePath: String) {

            private val file by lazy {
                WbFileDriver.createFile(uri(filePath))
                    .createWbFileOutputStream()
            }
            private val bytesPerSample = BitDepth.BIT_32.bytesPerSample
            private val bitDepth = BitDepth.BIT_32

            operator fun invoke(argument: WriteFunctionArgument<T>): Boolean {
                when (argument.phase) {
                    WRITE -> {
                        when (argument.sampleClazz) {
                            Sample::class -> {
                                val element = argument.sample!! as Sample
                                val buffer = ByteArray(bytesPerSample)
                                buffer.encodeSampleLEBytes(0, element, bitDepth)
                                file.write(buffer)
                            }

                            SampleVector::class -> {
                                val element = argument.sample!! as SampleVector
                                val buffer = ByteArray(bytesPerSample * element.size)
                                for (i in element.indices) {
                                    buffer.encodeSampleLEBytes(i * bytesPerSample, element[i], bitDepth)
                                }
                                file.write(buffer)
                            }

                            else -> fail("Unsupported $argument")
                        }
                    }

                    CLOSE -> file.close()
                    END -> {
                        /*nothing to do*/
                    }
                }
                return true
            }
        }

        val input = (440.sine() * 0.2).map { it * 2 }.trim(2000)
        val sampleRate = 4000.0f
        lateinit var outputFile: File
        lateinit var generated: List<Sample>

        beforeTest {
            outputFile = File.createTempFile("temp", ".raw")
            generated = ByteArrayLittleEndianInput(
                ByteArrayLittleEndianInputParams(
                    sampleRate,
                    BitDepth.BIT_32,
                    outputFile.readBytes()
                )
            ).toList(sampleRate)
        }

        context("should store sample bytes as LE into a file") {
            withData(modes) { (mode, locateFacilitators, evaluate) ->
                val encoder = FileEncoderFn<Sample>("file://${outputFile.absolutePath}")
                val o = input.out { encoder(it) }
                evaluate(o, sampleRate, locateFacilitators())

                assertThat(generated).isContainedBy(input.toList(sampleRate)) { a, b -> abs(a - b) < 1e-8 }
            }
        }
        context("should store sample vector bytes as LE into a file") {
            withData(modes) { (mode, locateFacilitators, evaluate) ->
                val encoder = FileEncoderFn<SampleVector>("file://${outputFile.absolutePath}")
                val o = input
                    .window(64).map { sampleVectorOf(it) }
                    .out { encoder(it) }
                evaluate(o, sampleRate, locateFacilitators())

                assertThat(generated).isContainedBy(input.toList(sampleRate)) { a, b -> abs(a - b) < 1e-8 }
            }
        }
    }
})