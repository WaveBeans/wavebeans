package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.wavebeans.tests.eachIndexed
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import io.wavebeans.lib.URI
import java.nio.file.Files
import kotlin.math.absoluteValue
import kotlin.random.Random

object FileWriterDelegateSpec : Spek({

    beforeGroup {
        TestWbFileDriver.register()
    }

    afterGroup {
        TestWbFileDriver.unregister()
    }

    describe("Single file") {
        val outputFile by memoized(TEST) { TestWbFileDriver.createTempFile() }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>(
                    uriGenerationStrategy = { URI(outputFile.url) },
                    bufferSize = 128,
                    localFileFactory = TestWbFileDriver.driver
            )
                    .also { it.initBuffer(null) }
        }

        it("should store short buffer without headers") {
            val input = "1234567890"
            delegate.write(input.toByteArray())
            delegate.close()

            assertThat(outputFile.readText()).isEqualTo(input)
        }
        it("should store buffer longer than internal buffer without headers") {
            val input = (0..100).joinToString("") { "1234567890" }
            delegate.write(input.toByteArray())
            delegate.close()

            assertThat(outputFile.readText()).isEqualTo(input)
        }

        it("should store buffer longer than internal buffer with header and footer") {
            val input = (0..100).joinToString("") { "1234567890" }
            val header = "header1234567890"
            val footer = "footer1234567890"
            delegate.write(input.toByteArray())
            delegate.close({ header.toByteArray() }, { footer.toByteArray() })

            assertThat(outputFile.readText()).isEqualTo(header + input + footer)
        }
    }

    describe("Multiple files with flush") {
        val outputFiles by memoized(TEST) { ArrayList<TestFile>() }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>(
                    uriGenerationStrategy = {
                        val outputFile = TestWbFileDriver.createTempFile()
                        outputFiles += outputFile
                        URI(outputFile.url)
                    },
                    localFileFactory = TestWbFileDriver.driver,
                    bufferSize = 128).also { it.initBuffer(null) }
        }
        val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

        it("should store a few short files without headers and footers") {

            delegate.performWritesWithFlush(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
            }
        }

        it("should store a few short files with headers and footers") {
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.performWritesWithFlush(contents, header, footer)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
            }
        }
    }

    describe("Multiple files with manual buffer manipulation") {
        val outputFiles by memoized(TEST) { ArrayList<TestFile>() }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>({
                val outputFile = TestWbFileDriver.createTempFile()
                outputFiles += outputFile
                URI(outputFile.url)
            },
                    localFileFactory = TestWbFileDriver.driver,
                    bufferSize = 128).also { it.initBuffer(null) }
        }

        val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

        it("should store a few short files without headers and footers") {
            delegate.performWritesWithManualBufferManagement(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
            }
        }

        it("should store a few short files with headers and footers") {
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.performWritesWithManualBufferManagement(contents, header, footer)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
            }
        }
    }

    describe("Suffixed file writer") {
        val directory by memoized(TEST) { Random.nextLong().absoluteValue.toString(36) }
        val delegate by memoized(TEST) {
            var i = 0
            suffixedFileWriterDelegate<Unit>("test:///$directory/test.out", localFileFactory = TestWbFileDriver.driver) {
                (i++).toString(16)
            }.also { it.initBuffer(null) }
        }
        val outputFiles by memoized(TEST) { TestWbFileDriver.listFiles(directory).sortedBy { it.url } }
        val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

        it("should store a few short files without headers and footers") {

            delegate.performWritesWithFlush(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
                file.prop("url") { it.url }.isEqualTo("test:///$directory/test$index.out")
            }
        }

        it("should store a few short files with headers and footers") {
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.performWritesWithFlush(contents, header, footer)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
                file.prop("url") { it.url }.isEqualTo("test:///$directory/test$index.out")
            }
        }
    }
})

private val log = KotlinLogging.logger { }

private fun WriterDelegate<Unit>.performWritesWithFlush(contents: List<String>, header: String? = null, footer: String? = null) {
    contents.forEach {
        log.debug { "Writing buffer value=$it" }
        this.write(it.toByteArray())
        this.flush(null, { header?.toByteArray() }, { footer?.toByteArray() })
    }
    this.close({ header?.toByteArray() }, { footer?.toByteArray() })
}


private fun WriterDelegate<Unit>.performWritesWithManualBufferManagement(contents: List<String>, header: String? = null, footer: String? = null) {
    contents.forEachIndexed { index, value ->
        log.debug { "Writing buffer index=$index, value=$value" }
        if (index > 0)
            this.initBuffer(null)
        this.write(value.toByteArray())
        if (index < contents.size - 1)
            this.finalizeBuffer(null, { header?.toByteArray() }, { footer?.toByteArray() })
    }
    this.close({ header?.toByteArray() }, { footer?.toByteArray() })
}

