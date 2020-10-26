package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.wavebeans.lib.eachIndexed
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.net.URI
import java.nio.file.Files

object FileWriterDelegateSpec : Spek({


    describe("Single file") {
        val outputFile by memoized(TEST) { File.createTempFile("test", ".out").also { it.deleteOnExit() } }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>({ URI("file://${outputFile.absolutePath}") }, bufferSize = 128)
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
            delegate.headerFn = { header.toByteArray() }
            delegate.footerFn = { footer.toByteArray() }
            delegate.write(input.toByteArray())
            delegate.close()

            assertThat(outputFile.readText()).isEqualTo(header + input + footer)
        }
    }

    describe("Multiple files with flush") {
        val outputFiles by memoized(TEST) { ArrayList<File>() }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                URI("file://${outputFile.absolutePath}")
            }, bufferSize = 128).also { it.initBuffer(null) }
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

            delegate.headerFn = { header.toByteArray() }
            delegate.footerFn = { footer.toByteArray() }

            delegate.performWritesWithFlush(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
            }
        }
    }

    describe("Multiple files with manual buffer manipulation") {
        val outputFiles by memoized(TEST) { ArrayList<File>() }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                URI("file://${outputFile.absolutePath}")
            }, bufferSize = 128).also { it.initBuffer(null) }
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

            delegate.headerFn = { header.toByteArray() }
            delegate.footerFn = { footer.toByteArray() }

            delegate.performWritesWithManualBufferManagement(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
            }
        }
    }

    describe("Suffixed file writer") {
        val directory by memoized(TEST) { Files.createTempDirectory("tmp").toFile() }
        val delegate by memoized(TEST) {
            var i = 0
            suffixedFileWriterDelegate<Unit>("file://${directory.absolutePath}/test.out") {
                (i++).toString(16)
            }.also { it.initBuffer(null) }
        }
        val outputFiles by memoized(TEST) {
            directory.listFiles()
                    ?.map { it!! }
                    ?.sortedBy { it.name }
                    ?: emptyList()
        }
        val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

        it("should store a few short files without headers and footers") {

            delegate.performWritesWithFlush(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
                file.prop("name") { it.name }.isEqualTo("test$index.out")
                file.prop("parent") { it.parent }.isEqualTo(directory.path)
            }
        }

        it("should store a few short files with headers and footers") {
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.headerFn = { header.toByteArray() }
            delegate.footerFn = { footer.toByteArray() }

            delegate.performWritesWithFlush(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
                file.prop("name") { it.name }.isEqualTo("test$index.out")
                file.prop("parent") { it.parent }.isEqualTo(directory.path)
            }
        }
    }
})

private val log = KotlinLogging.logger { }

private fun WriterDelegate<Unit>.performWritesWithFlush(contents: List<String>) {
    contents.forEach {
        log.debug {"Writing buffer value=$it"}
        this.write(it.toByteArray())
        this.flush(null)
    }
    this.close()
}


private fun WriterDelegate<Unit>.performWritesWithManualBufferManagement(contents: List<String>) {
    contents.forEachIndexed { index, value ->
        log.debug {"Writing buffer index=$index, value=$value"}
        if (index > 0)
            this.initBuffer(null)
        this.write(value.toByteArray())
        if (index < contents.size - 1)
            this.finalizeBuffer(null)
    }
    this.close()
}

