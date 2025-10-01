package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.tests.eachIndexed
import mu.KotlinLogging
import java.io.File
import java.net.URI
import java.nio.file.Files

class FileWriterDelegateSpec : DescribeSpec({

    describe("Single file") {
        it("should store short buffer without headers") {
            val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
            val delegate = FileWriterDelegate<Unit>({ outputFile.toURI() }, bufferSize = 128)
                .also { it.initBuffer(null) }

            val input = "1234567890"
            delegate.write(input.toByteArray())
            delegate.close()

            assertThat(outputFile.readText()).isEqualTo(input)
        }
        it("should store buffer longer than internal buffer without headers") {
            val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
            val delegate = FileWriterDelegate<Unit>({ outputFile.toURI() }, bufferSize = 128)
                .also { it.initBuffer(null) }

            val input = (0..100).joinToString("") { "1234567890" }
            delegate.write(input.toByteArray())
            delegate.close()

            assertThat(outputFile.readText()).isEqualTo(input)
        }

        it("should store buffer longer than internal buffer with header and footer") {
            val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
            val delegate = FileWriterDelegate<Unit>({ outputFile.toURI() }, bufferSize = 128)
                .also { it.initBuffer(null) }

            val input = (0..100).joinToString("") { "1234567890" }
            val header = "header1234567890"
            val footer = "footer1234567890"
            delegate.write(input.toByteArray())
            delegate.close({ header.toByteArray() }, { footer.toByteArray() })

            assertThat(outputFile.readText()).isEqualTo(header + input + footer)
        }
    }

    describe("Multiple files with flush") {
        it("should store a few short files without headers and footers") {
            val outputFiles = ArrayList<File>()
            val delegate = FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                outputFile.toURI()
            }, bufferSize = 128).also { it.initBuffer(null) }

            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

            delegate.performWritesWithFlush(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
            }
        }

        it("should store a few short files with headers and footers") {
            val outputFiles = ArrayList<File>()
            val delegate = FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                outputFile.toURI()
            }, bufferSize = 128).also { it.initBuffer(null) }

            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.performWritesWithFlush(contents, header, footer)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
            }
        }
    }

    describe("Multiple files with manual buffer manipulation") {
        it("should store a few short files without headers and footers") {
            val outputFiles = ArrayList<File>()
            val delegate = FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                outputFile.toURI()
            }, bufferSize = 128).also { it.initBuffer(null) }

            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

            delegate.performWritesWithManualBufferManagement(contents)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
            }
        }

        it("should store a few short files with headers and footers") {
            val outputFiles = ArrayList<File>()
            val delegate = FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                outputFile.toURI()
            }, bufferSize = 128).also { it.initBuffer(null) }

            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.performWritesWithManualBufferManagement(contents, header, footer)

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
            }
        }
    }

    describe("Suffixed file writer") {
        it("should store a few short files without headers and footers") {
            val directory = Files.createTempDirectory("tmp").toFile()
            val delegate = run {
                var i = 0
                suffixedFileWriterDelegate<Unit>("file://${directory.absolutePath}/test.out") {
                    (i++).toString(16)
                }.also { it.initBuffer(null) }
            }
            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

            delegate.performWritesWithFlush(contents)

            val outputFiles = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
                file.prop("name") { it.name }.isEqualTo("test$index.out")
                file.prop("parent") { it.parent }.isEqualTo(directory.path)
            }
        }

        it("should store a few short files with headers and footers") {
            val directory = Files.createTempDirectory("tmp").toFile()
            val delegate = run {
                var i = 0
                suffixedFileWriterDelegate<Unit>("file://${directory.absolutePath}/test.out") {
                    (i++).toString(16)
                }.also { it.initBuffer(null) }
            }
            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.performWritesWithFlush(contents, header, footer)

            val outputFiles = directory.listFiles()?.map { it!! }?.sortedBy { it.name } ?: emptyList()

            assertThat(outputFiles).eachIndexed(contents.size) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
                file.prop("name") { it.name }.isEqualTo("test$index.out")
                file.prop("parent") { it.parent }.isEqualTo(directory.path)
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

