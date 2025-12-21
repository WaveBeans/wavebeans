package io.wavebeans.lib.io

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.fail
import io.wavebeans.lib.*
import io.wavebeans.lib.io.WriteFunctionPhase.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.evaluate
import io.wavebeans.tests.isContainedBy
import io.wavebeans.tests.toList
import io.kotest.core.spec.style.DescribeSpec
import java.io.File
import kotlin.math.abs

private object IntStorage {
    private val list = ArrayList<Int>()

    fun add(sample: Int) {
        list += sample
    }

    fun reset() {
        list.clear()
    }

    fun list(): List<Int> = list
}

class FunctionStreamOutputSpec : DescribeSpec({

    describe("Writing integers") {

        beforeTest {
            IntStorage.reset()
        }

        val input = input { x, _ -> x.toInt() }.trim(100)

        it("should write till the end of the stream") {
            input.out {
                if (it.sampleClazz != Int::class) fail("int sample required but ${it.sampleClazz} found")
                when (it.phase) {
                    WRITE -> IntStorage.add(it.sample!!)
                    END -> IntStorage.add(-1)
                    CLOSE -> IntStorage.add(-2)
                }
                true
            }.evaluate(1000.0f)

            assertThat(IntStorage.list()).all {
                size().isEqualTo(102)
                isEqualTo((0..99).toList() + -1 + -2)
            }
        }
        it("should end processing when the function returned false") {
            input.out {
                if (it.sampleClazz != Int::class) fail("int sample required but ${it.sampleClazz} found")
                when (it.phase) {
                    WRITE -> IntStorage.add(it.sample!!)
                    END -> IntStorage.add(-1)
                    CLOSE -> IntStorage.add(-2)
                }
                it.sampleIndex < 49
            }.evaluate(1000.0f)

            assertThat(IntStorage.list()).all {
                size().isEqualTo(51)
                isEqualTo((0..49).toList() + -2)
            }
        }
        it("should not continue writing if closed right away") {
            input.out {
                if (it.sampleClazz != Int::class) fail("int sample required but ${it.sampleClazz} found")
                if (it.sampleIndex > 0) fail("unreachable")
                when (it.phase) {
                    WRITE -> IntStorage.add(it.sample!!)
                    END -> IntStorage.add(-1)
                    CLOSE -> IntStorage.add(-2)
                }
                false
            }.evaluate(1000.0f)

            assertThat(IntStorage.list()).isEqualTo(listOf(0, -2))
        }
        it("should throw an exception properly and the output processing should end") {
            assertThat(runCatching {
                input.out {
                    if (it.sampleIndex == 5L) throw IllegalStateException("some exception")
                    when (it.phase) {
                        WRITE -> IntStorage.add(it.sample!!)
                        END -> IntStorage.add(-1)
                        CLOSE -> IntStorage.add(-2)
                    }
                    true
                }.evaluate(1000.0f)
            })
                .isFailure()
                .isNotNull().message().isEqualTo("some exception")
            assertThat(IntStorage.list()).isEqualTo((0..4).toList())
        }
    }

    describe("Writing encoded samples") {

        class FileEncoderFn<T : Any>(file: String) : Fn<WriteFunctionArgument<T>, Boolean>(
            FnInitParameters().add("file", file)
        ) {

            private val file by lazy { File(initParams.string("file")).outputStream().buffered() }
            private val bytesPerSample = BitDepth.BIT_32.bytesPerSample
            private val bitDepth = BitDepth.BIT_32

            override fun apply(argument: WriteFunctionArgument<T>): Boolean {
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
                        /** nothing to do */
                    }
                }
                return true
            }
        }

        fun <T : Any> streamEncoder(stream: java.io.OutputStream, argument: WriteFunctionArgument<T>): Boolean {
            val bytesPerSample = BitDepth.BIT_32.bytesPerSample
            val bitDepth = BitDepth.BIT_32
            when (argument.phase) {
                WRITE -> {
                    when (argument.sampleClazz) {
                        Sample::class -> {
                            val element = argument.sample!! as Sample
                            val buffer = ByteArray(bytesPerSample)
                            buffer.encodeSampleLEBytes(0, element, bitDepth)
                            stream.write(buffer)
                        }

                        SampleVector::class -> {
                            val element = argument.sample!! as SampleVector
                            val buffer = ByteArray(bytesPerSample * element.size)
                            for (i in element.indices) {
                                buffer.encodeSampleLEBytes(i * bytesPerSample, element[i], bitDepth)
                            }
                            stream.write(buffer)
                        }

                        else -> fail("Unsupported $argument")
                    }
                }

                CLOSE -> stream.close()
                END -> {
                    /** nothing to do */
                }
            }
            return true
        }

        val input = 440.sine().trim(10)
        val sampleRate = 4000.0f

        it("should store sample bytes as LE into a file") {
            val outputFile = File.createTempFile("temp", ".raw").also { it.deleteOnExit() }
            val stream = outputFile.outputStream().buffered()
            input.out { streamEncoder(stream, it) }.evaluate(sampleRate)

            val generated = ByteArrayLittleEndianInput(
                ByteArrayLittleEndianInputParams(
                    sampleRate,
                    BitDepth.BIT_32,
                    outputFile.readBytes()
                )
            ).toList(sampleRate)

            assertThat(generated).isContainedBy(input.toList(sampleRate)) { a, b -> abs(a - b) < 1e-8 }
        }
        it("should store sample vector bytes as LE into a file") {
            val outputFile = File.createTempFile("temp", ".raw").also { it.deleteOnExit() }
            val stream = outputFile.outputStream().buffered()
            input.window(64)
                .map { sampleVectorOf(it) }
                .out { streamEncoder(stream, it) }
                .evaluate(sampleRate)

            val generated = ByteArrayLittleEndianInput(
                ByteArrayLittleEndianInputParams(
                    sampleRate,
                    BitDepth.BIT_32,
                    outputFile.readBytes()
                )
            ).toList(sampleRate)

            assertThat(generated).isContainedBy(input.toList(sampleRate)) { a, b -> abs(a - b) < 1e-8 }
        }
    }
})