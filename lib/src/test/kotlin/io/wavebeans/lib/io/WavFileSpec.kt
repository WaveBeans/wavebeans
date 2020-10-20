package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.prop
import io.wavebeans.lib.*
import io.wavebeans.lib.BitDepth.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.merge
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.nio.file.Files
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import kotlin.random.Random

private const val sampleRate = 192000.0f

object WavFileSpec : Spek({
    describe("Writing mono") {
        val input = 440.sine().trim(2)
        val expectedSize = 384

        val file by memoized(TEST) { File.createTempFile("wavtest", ".wav") }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {

                it("should read the same sine") {
                    run(input, bitDepth, file)
                    val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                    assertThat(wave("file://${file.absolutePath}").asSequence(sampleRate).toList())
                            .eachIndexed(expectedSize) { sample, idx ->
                                sample.isCloseTo(inputAsArray[idx], precision)
                            }
                }

                it("should read the same sine when sampled with SampleArray") {
                    run(input.window(64).map { sampleArrayOf(it) }, bitDepth, file)
                    val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                    assertThat(wave("file://${file.absolutePath}").asSequence(sampleRate).toList())
                            .eachIndexed(expectedSize) { sample, idx ->
                                sample.isCloseTo(inputAsArray[idx], precision)
                            }
                }
            }
        }
    }

    describe("Partial flush on sample level") {
        data class IndexedSample(
                val sample: Sample,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

        fun run(input: BeanStream<Sample>, durationMs: Long, chunkSize: Int, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters) : Fn<IndexedSample, Managed<OutputSignal, TemporalAccessor, Sample>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSample): Managed<OutputSignal, TemporalAccessor, Sample> {
                    val cz = initParams.int("chunkSize")
                    return if (cz > 0 && argument.index > 0 && argument.index % cz == 0L) {
                        argument.sample.withOutputSignal(FlushOutputSignal, ZonedDateTime.now())
                    } else {
                        argument.sample.withOutputSignal(NoopOutputSignal, null)
                    }
                }
            }

            val o = input
                    .merge(input { it.first }) { (sample, index) ->
                        checkNotNull(sample)
                        checkNotNull(index)
                        IndexedSample(sample, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(durationMs)
                    .let {
                        val suffix: (TemporalAccessor?) -> String = { a ->
                            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                            "-${dtf.format(a ?: ZonedDateTime.now())}-${Random.nextInt(Int.MAX_VALUE).toString(32)}"
                        }
                        val uri = "file://${directory.absolutePath}/test.wav"
                        when (bitDepth) {
                            BIT_8 -> it.toMono8bitWav(uri, suffix)
                            BIT_16 -> it.toMono16bitWav(uri, suffix)
                            BIT_24 -> it.toMono24bitWav(uri, suffix)
                            BIT_32 -> it.toMono32bitWav(uri, suffix)
                            else -> throw UnsupportedOperationException()
                        }
                    }
            o.writer(sampleRate).use {
                while (it.write()) {
                }
            }
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                it("should read the same input out of 5 files one by one") {
                    val chunkSize = 384
                    val input = seqStream()
                    val chunksCount = 5
                    run(input, (chunksCount * 2).toLong(), chunkSize, bitDepth)
                    val expected = input.asSequence(sampleRate).take(chunkSize * chunksCount).toList()
                    assertThat(outputFiles()).eachIndexed(chunksCount) { file, index ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(chunkSize) { v, i -> v.isCloseTo(expected[chunkSize * index + i], precision) }
                    }
                }
                it("should read the same input out of 1 file as it wasn't flushed") {
                    val input = seqStream()
                    run(input, 10, 0, bitDepth)
                    val n = 1920
                    val expected = input.asSequence(sampleRate).take(n).toList()
                    assertThat(outputFiles()).eachIndexed(1) { file, _ ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(n) { v, i -> v.isCloseTo(expected[i], precision) }
                    }
                }
            }
        }
    }

    describe("Partial flush on sample array level") {
        data class IndexedSampleArray(
                val sample: SampleArray,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()
        val windowSize = 128

        fun run(input: BeanStream<Sample>, durationMs: Long, chunkSize: Int, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters) : Fn<IndexedSampleArray, Managed<OutputSignal, TemporalAccessor, SampleArray>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSampleArray): Managed<OutputSignal, TemporalAccessor, SampleArray> {
                    val cz = initParams.int("chunkSize")
                    return if (cz > 0 && argument.index > 0 && argument.index % cz == 0L) {
                        argument.sample.withOutputSignal(FlushOutputSignal, ZonedDateTime.now())
                    } else {
                        argument.sample.withOutputSignal(NoopOutputSignal, null)
                    }
                }
            }

            val o = input
                    .window(windowSize)
                    .map { sampleArrayOf(it) }
                    .merge(input { it.first }) { (sampleArray, index) ->
                        checkNotNull(sampleArray)
                        checkNotNull(index)
                        IndexedSampleArray(sampleArray, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(durationMs)
                    .let {
                        val suffix: (TemporalAccessor?) -> String = { a ->
                            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                            "-${dtf.format(a ?: ZonedDateTime.now())}-${Random.nextInt(Int.MAX_VALUE).toString(32)}"
                        }
                        val uri = "file://${directory.absolutePath}/test.wav"
                        when (bitDepth) {
                            BIT_8 -> it.toMono8bitWav(uri, suffix)
                            BIT_16 -> it.toMono16bitWav(uri, suffix)
                            BIT_24 -> it.toMono24bitWav(uri, suffix)
                            BIT_32 -> it.toMono32bitWav(uri, suffix)
                            else -> throw UnsupportedOperationException()
                        }
                    }
            o.writer(sampleRate).use {
                while (it.write()) {
                }
            }
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                it("should read the same input out of 5 files one by one") {
                    val uberChunkSize = 3
                    val chunkSize = uberChunkSize * windowSize
                    val chunksCount = 5
                    val input = seqStream()
                    run(input, (chunksCount * 2).toLong(), uberChunkSize, bitDepth)
                    val expected = input.asSequence(sampleRate).take(chunkSize * chunksCount).toList()
                    assertThat(outputFiles()).eachIndexed(chunksCount) { file, index ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(chunkSize) { v, i ->
                                    v.isCloseTo(expected[chunkSize * index + i], precision)
                                }
                    }
                }
                it("should read the same input out of 1 file as it wasn't flushed") {
                    val input = seqStream()
                    run(input, 10, 0, bitDepth)
                    val n = 15 * windowSize
                    val expected = input.asSequence(sampleRate).take(n).toList()
                    assertThat(outputFiles()).eachIndexed(1) { file, _ ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(n) { v, i -> v.isCloseTo(expected[i], precision) }
                    }
                }
            }
        }
    }
})

private inline fun <reified T : Any> run(input: BeanStream<T>, bitDepth: BitDepth, file: File) {
    val uri = "file://${file.absolutePath}"
    val o = when (bitDepth) {
        BIT_8 -> input.toMono8bitWav(uri)
        BIT_16 -> input.toMono16bitWav(uri)
        BIT_24 -> input.toMono24bitWav(uri)
        BIT_32 -> input.toMono32bitWav(uri)
        else -> throw UnsupportedOperationException()
    }
    o.writer(sampleRate).use {
        while (it.write()) {
        }
    }
}

