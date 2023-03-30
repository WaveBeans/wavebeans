package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.wavebeans.lib.*
import io.wavebeans.lib.BitDepth.*
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.merge
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.eachIndexed
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.nio.file.Files
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

private const val sampleRate = 192000.0f
private val log = KotlinLogging.logger { }

object WavFileSpec : Spek({
    describe("Writing mono") {
        val input = 440.sine().trim(2)
        val expectedSize = 384

        val file by memoized(TEST) { File.createTempFile("wavtest", ".wav") }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {

                it("should read the same sine") {
                    evaluate(input, bitDepth, file)
                    val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                    assertThat(wave("file://${file.absolutePath}").asSequence(sampleRate).toList())
                            .eachIndexed(expectedSize) { sample, idx ->
                                sample.isCloseTo(inputAsArray[idx], precision)
                            }
                }

                it("should read the same sine when sampled with SampleVector") {
                    evaluate(input.window(64).map { sampleVectorOf(it) }, bitDepth, file)
                    val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                    assertThat(wave("file://${file.absolutePath}").asSequence(sampleRate).toList())
                            .eachIndexed(expectedSize) { sample, idx ->
                                sample.isCloseTo(inputAsArray[idx], precision)
                            }
                }
            }
        }
    }

    describe("Partial flush on sample stream") {
        data class IndexedSample(
                val sample: Sample,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

        fun run(input: BeanStream<Sample>, durationMs: Long, chunkSize: Int, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters) : Fn<IndexedSample, Managed<OutputSignal, Pair<TemporalAccessor, Long>, Sample>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSample): Managed<OutputSignal, Pair<TemporalAccessor, Long>, Sample> {
                    val cz = initParams.int("chunkSize")
                    return if (cz > 0 && argument.index > 0 && argument.index % cz == 0L) {
                        argument.sample.withOutputSignal(FlushOutputSignal, ZonedDateTime.now() to argument.index)
                    } else {
                        argument.sample.withOutputSignal(NoopOutputSignal, null)
                    }
                }
            }

            val suffix: (Pair<TemporalAccessor, Long>?) -> String = { a ->
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                "-${dtf.format(a?.first ?: ZonedDateTime.now())}-${a?.second ?: 0}"
            }
            val uri = "file://${directory.absolutePath}/test.wav"
            val o = input
                    .merge(input { it.first }) { (sample, index) ->
                        checkNotNull(sample)
                        checkNotNull(index)
                        IndexedSample(sample, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(durationMs)
            evaluate(o, bitDepth, uri, suffix)
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                val input by memoized(TEST) { seqStream() }
                val chunkSize = 384
                val chunksCount = 5
                val overallSize = 1920
                val overallLengthMs = 10L

                it("should read the same input out of 5 files one by one") {
                    run(input, overallLengthMs, chunkSize, bitDepth)
                    val expected = input.asSequence(sampleRate).take(chunkSize * chunksCount).toList()
                    assertThat(outputFiles()).eachIndexed(chunksCount) { file, index ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(chunkSize) { v, i -> v.isCloseTo(expected[chunkSize * index + i], precision) }
                    }
                }
                it("should read the same input out of 1 file as it wasn't flushed") {
                    run(input, overallLengthMs, 0, bitDepth)
                    val expected = input.asSequence(sampleRate).take(overallSize).toList()
                    assertThat(outputFiles()).eachIndexed(1) { file, _ ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(overallSize) { v, i -> v.isCloseTo(expected[i], precision) }
                    }
                }
            }
        }
    }

    describe("Partial flush on sample vector stream") {
        data class IndexedSampleVector(
                val sample: SampleVector,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()
        val windowSize = 128

        fun run(input: BeanStream<Sample>, durationMs: Long, chunkSize: Int, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters) : Fn<IndexedSampleVector, Managed<OutputSignal, Pair<TemporalAccessor, Long>, SampleVector>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSampleVector): Managed<OutputSignal, Pair<TemporalAccessor, Long>, SampleVector> {
                    val cz = initParams.int("chunkSize")
                    return if (cz > 0 && argument.index > 0 && argument.index % cz == 0L) {
                        argument.sample.withOutputSignal(FlushOutputSignal, ZonedDateTime.now() to argument.index)
                    } else {
                        argument.sample.withOutputSignal(NoopOutputSignal, null)
                    }
                }
            }

            val suffix: (Pair<TemporalAccessor, Long>?) -> String = { a ->
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                "-${dtf.format(a?.first ?: ZonedDateTime.now())}-${a?.second ?: 0}"
            }
            val uri = "file://${directory.absolutePath}/test.wav"
            val o = input
                    .window(windowSize)
                    .map { sampleVectorOf(it) }
                    .merge(input { it.first }) { (sampleVector, index) ->
                        checkNotNull(sampleVector)
                        checkNotNull(index)
                        IndexedSampleVector(sampleVector, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(durationMs)
            evaluate(o, bitDepth, uri, suffix)
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                val input by memoized(TEST) { seqStream() }
                val uberChunkSize = 3
                val chunkSize = uberChunkSize * windowSize
                val chunksCount = 5
                val overallSize = 15 * windowSize
                val overallLengthMs = 10L

                it("should read the same input out of 5 files one by one") {
                    run(input, overallLengthMs, uberChunkSize, bitDepth)
                    val expected = input.asSequence(sampleRate).take(chunkSize * chunksCount).toList()
                    assertThat(outputFiles()).eachIndexed(chunksCount) { file, index ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(chunkSize) { v, i ->
                                    v.isCloseTo(expected[chunkSize * index + i], precision)
                                }
                    }
                }
                it("should read the same input out of 1 file as it wasn't flushed") {
                    run(input, overallLengthMs, 0, bitDepth)
                    val expected = input.asSequence(sampleRate).take(overallSize).toList()
                    assertThat(outputFiles()).eachIndexed(1) { file, _ ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(overallSize) { v, i -> v.isCloseTo(expected[i], precision) }
                    }
                }
            }
        }
    }

    describe("Open-close gate on sample stream") {
        data class IndexedSample(
                val sample: Sample,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

        fun run(input: BeanStream<Sample>, durationMs: Long, chunkSize: Int, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters) : Fn<IndexedSample, Managed<OutputSignal, Pair<TemporalAccessor, Long>, Sample>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSample): Managed<OutputSignal, Pair<TemporalAccessor, Long>, Sample> {
                    val cz = initParams.int("chunkSize")
                    return if (cz > 0 && argument.index > 0 && argument.index % cz == 0L) {
                        // we'll write only even chunks
                        if (argument.index / cz % 2 == 1L)
                            argument.sample.withOutputSignal(CloseGateOutputSignal, ZonedDateTime.now() to argument.index)
                        else
                            argument.sample.withOutputSignal(OpenGateOutputSignal, ZonedDateTime.now() to argument.index)
                    } else {
                        argument.sample.withOutputSignal(NoopOutputSignal, null)
                    }
                }
            }

            val suffix: (Pair<TemporalAccessor, Long>?) -> String = { a ->
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                "-${dtf.format(a?.first ?: ZonedDateTime.now())}-${a?.second ?: 0}"
            }
            val uri = "file://${directory.absolutePath}/test.wav"
            val o = input
                    .merge(input { it.first }) { (sample, index) ->
                        checkNotNull(sample)
                        checkNotNull(index)
                        IndexedSample(sample, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(durationMs)
            evaluate(o, bitDepth, uri, suffix)
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                val input by memoized(TEST) { seqStream() }
                val chunkSize = 384
                val chunksCount = 5
                val filesCount = 3
                val overallSize = 1920
                val overallLengthMs = 10L

                it("should read only even chunks out of 3 files") {
                    run(input, overallLengthMs, chunkSize, bitDepth)
                    val expected = input.asSequence(sampleRate).take(chunkSize * chunksCount).toList()
                    assertThat(outputFiles()).eachIndexed(filesCount) { file, index ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(chunkSize) { v, i -> v.isCloseTo(expected[chunkSize * index * 2 + i], precision) }
                    }
                }
                it("should read the same input out of 1 file as it wasn't flushed") {
                    run(input, overallLengthMs, 0, bitDepth)
                    val expected = input.asSequence(sampleRate).take(overallSize).toList()
                    assertThat(outputFiles()).eachIndexed(1) { file, _ ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(overallSize) { v, i -> v.isCloseTo(expected[i], precision) }
                    }
                }
            }
        }
    }

    describe("Consequent open-close gate signals on sample stream") {
        data class IndexedSample(
                val sample: Sample,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

        fun run(input: BeanStream<Sample>, durationMs: Long, chunkSize: Int, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters) : Fn<IndexedSample, Managed<OutputSignal, Pair<TemporalAccessor, Long>, Sample>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSample): Managed<OutputSignal, Pair<TemporalAccessor, Long>, Sample> {
                    val cz = initParams.int("chunkSize")
                    // close or open gate is sent with each sample, but only the first one actually makes difference
                    // the effect is the same as to send open/close gate signal on the very fisrt chunk
                    // and then sending noop in between.
                    return if (argument.index / cz % 2 == 0L)
                        argument.sample.withOutputSignal(OpenGateOutputSignal, ZonedDateTime.now() to argument.index)
                    else
                        argument.sample.withOutputSignal(CloseGateOutputSignal, ZonedDateTime.now() to argument.index)
                }
            }

            val suffix: (Pair<TemporalAccessor, Long>?) -> String = { a ->
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
                "-${dtf.format(a?.first ?: ZonedDateTime.now())}-${a?.second ?: 0}"
            }
            val uri = "file://${directory.absolutePath}/test.wav"
            val o = input
                    .merge(input { it.first }) { (sample, index) ->
                        checkNotNull(sample)
                        checkNotNull(index)
                        IndexedSample(sample, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(durationMs)
            evaluate(o, bitDepth, uri, suffix)
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                val input by memoized(TEST) { seqStream() }
                val chunkSize = 384
                val chunksCount = 5
                val filesCount = 3
                val overallLengthMs = 10L

                it("should read only even chunks out of 3 files") {
                    run(input, overallLengthMs, chunkSize, bitDepth)
                    val expected = input.asSequence(sampleRate).take(chunkSize * chunksCount).toList()
                    assertThat(outputFiles()).eachIndexed(filesCount) { file, index ->
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(chunkSize) { v, i -> v.isCloseTo(expected[chunkSize * index * 2 + i], precision) }
                    }
                }
            }
        }
    }

    describe("Open-close gate mixed with flush on sample stream") {
        data class IndexedSample(
                val sample: Sample,
                val index: Long
        )

        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        fun outputFiles() = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

        val chunkSize = 384 // 2ms
        val overallLengthMs = 24L
        val filesCount = 3

        /**
         * 0 1 2 3 4 5 6 7 8 9 10 11  <- chunk #
         * --- flush on closed gate (flush do not open the gate)
         *     C                      -> chunks 0-1               (file #0)
         *         F                  -> no chunks
         * --- open after flush (gate is closed)
         *           O
         * --- flush on opened gate (flushes the current buffer)
         *             F             -> chunks 5                  (file #5)
         * --- close opened gate (flushes the remains)
         *                        C  -> chunk 6-10                (file #6)
         * --- closed the stream
         *                           -> nothing extra stored
         */
        fun run(input: BeanStream<Sample>, bitDepth: BitDepth) {
            class FlushController(params: FnInitParameters)
                : Fn<IndexedSample, Managed<OutputSignal, Long, Sample>>(params) {
                constructor(chunkSize: Int) : this(FnInitParameters().add("chunkSize", chunkSize))

                override fun apply(argument: IndexedSample): Managed<OutputSignal, Long, Sample> {
                    val cz = initParams.int("chunkSize")
                    val chunkNumber = argument.index / cz
                    return if (argument.index % cz == 0L) {
                        log.debug { "Detected next chunk chunkNumber=$chunkNumber argument.index=${argument.index}" }
                        when (chunkNumber) {
                            0L, 1L -> argument.sample.withOutputSignal(NoopOutputSignal)
                            2L -> argument.sample.withOutputSignal(CloseGateOutputSignal, chunkNumber)
                            3L -> argument.sample.withOutputSignal(NoopOutputSignal)
                            4L -> argument.sample.withOutputSignal(FlushOutputSignal, chunkNumber)
                            5L -> argument.sample.withOutputSignal(OpenGateOutputSignal, chunkNumber)
                            6L -> argument.sample.withOutputSignal(FlushOutputSignal, chunkNumber)
                            7L, 8L, 9L, 10L -> argument.sample.withOutputSignal(NoopOutputSignal)
                            11L -> argument.sample.withOutputSignal(CloseGateOutputSignal, chunkNumber)
                            else -> throw UnsupportedOperationException("$chunkNumber is unsupported")
                        }
                    } else {
                        argument.sample.withOutputSignal(NoopOutputSignal, null)
                    }
                }
            }

            val suffix: (Long?) -> String = { a -> "-${a ?: 0L}" }
            val uri = "file://${directory.absolutePath}/test.wav"
            val o = input
                    .merge(input { it.first }) { (sample, index) ->
                        checkNotNull(sample)
                        checkNotNull(index)
                        IndexedSample(sample, index)
                    }
                    .map(FlushController(chunkSize))
                    .trim(overallLengthMs)
            evaluate(o, bitDepth, uri, suffix)
        }

        listOf(BIT_8 to 1e-2, BIT_16 to 1e-4, BIT_24 to 1e-6, BIT_32 to 1e-9).forEach { (bitDepth, precision) ->
            describe("$bitDepth with precision $precision") {
                val input by memoized(TEST) { seqStream() }

                it("should read only even chunks out of 3 files") {
                    run(input, bitDepth)
                    fun expected(range: IntRange) = input.asSequence(sampleRate)
                            .drop(range.first * chunkSize)
                            .take(chunkSize * range.count())
                            .toList()
                    assertThat(outputFiles()).eachIndexed(filesCount) { file, index ->
                        val (suffix, elements) = when (index) {
                            0 -> "-0" to expected(0..1)
                            1 -> "-5" to expected(5..5)
                            2 -> "-6" to expected(6..10)
                            else -> throw UnsupportedOperationException("$index")
                        }
                        file.prop("name") { it.name }.isEqualTo("test$suffix.wav")
                        file.prop("content") { wave("file://${it.absolutePath}").asSequence(sampleRate).toList() }
                                .eachIndexed(elements.size) { v, i -> v.isCloseTo(elements[i], precision) }
                    }
                }
            }
        }
    }
})

private inline fun <reified T : Any> evaluate(input: BeanStream<T>, bitDepth: BitDepth, file: File) {
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

private inline fun <A : Any, reified T : Any> evaluate(
        input: BeanStream<Managed<OutputSignal, A, T>>,
        bitDepth: BitDepth,
        uri: String,
        noinline suffix: (A?) -> String
) {
    val o = when (bitDepth) {
        BIT_8 -> input.toMono8bitWav(uri, suffix)
        BIT_16 -> input.toMono16bitWav(uri, suffix)
        BIT_24 -> input.toMono24bitWav(uri, suffix)
        BIT_32 -> input.toMono32bitWav(uri, suffix)
        else -> throw UnsupportedOperationException()
    }
    o.writer(sampleRate).use {
        while (it.write()) {
        }
    }
}

