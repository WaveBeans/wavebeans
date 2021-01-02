package io.wavebeans.lib.io

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import assertk.fail
import io.wavebeans.lib.*
import io.wavebeans.lib.io.WriteFunctionPhase.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.evaluate
import io.wavebeans.tests.isContainedBy
import io.wavebeans.tests.toList
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.*
import org.spekframework.spek2.style.specification.describe
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

object FunctionStreamOutputSpec : Spek({

    describe("Writing integers") {

        beforeEachTest {
            IntStorage.reset()
        }

        val input = input { it.first.toInt() }.trim(100)

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
            val e = catch {
                input.out {
                    if (it.sampleIndex == 5L) throw IllegalStateException("some exception")
                    when (it.phase) {
                        WRITE -> IntStorage.add(it.sample!!)
                        END -> IntStorage.add(-1)
                        CLOSE -> IntStorage.add(-2)
                    }
                    true
                }.evaluate(1000.0f)
            }

            assertThat(e).isNotNull().message().isEqualTo("some exception")
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

        val input = 440.sine().trim(10)
        val sampleRate = 4000.0f
        val outputFile by memoized(TEST) { File.createTempFile("temp", ".raw") }
        val generated by memoized(TEST) {
            ByteArrayLittleEndianInput(ByteArrayLittleEndianInputParams(
                    sampleRate,
                    BitDepth.BIT_32,
                    outputFile.readBytes()
            )).toList(sampleRate)
        }

        it("should store sample bytes as LE into a file") {
            input.out(FileEncoderFn(outputFile.absolutePath)).evaluate(sampleRate)

            assertThat(generated).isContainedBy(input.toList(sampleRate)) { a, b -> abs(a - b) < 1e-8 }
        }
        it("should store sample vector bytes as LE into a file") {
            input.window(64).map { sampleVectorOf(it) }.out(FileEncoderFn(outputFile.absolutePath)).evaluate(sampleRate)

            assertThat(generated).isContainedBy(input.toList(sampleRate)) { a, b -> abs(a - b) < 1e-8 }
        }
    }
})