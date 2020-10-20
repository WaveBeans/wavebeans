package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.wavebeans.lib.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.net.URI
import java.nio.file.Files

object FileWriterDelegateSpec : Spek({
    describe("Single file") {
        val outputFile by memoized(TEST) { File.createTempFile("test", ".out").also { it.deleteOnExit() } }
        val delegate by memoized(TEST) { FileWriterDelegate<Unit>({ URI("file://${outputFile.absolutePath}") }, bufferSize = 128) }

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

    describe("Multiple files") {
        val outputFiles by memoized(TEST) { ArrayList<File>() }
        val delegate by memoized(TEST) {
            FileWriterDelegate<Unit>({
                val outputFile = File.createTempFile("test", ".out").also { it.deleteOnExit() }
                outputFiles += outputFile
                URI("file://${outputFile.absolutePath}")
            }, bufferSize = 128)
        }

        it("should store a few short files without headers and footers") {
            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

            delegate.performWrites(contents)

            assertThat(outputFiles).eachIndexed(4) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
            }
        }

        it("should store a few short files with headers and footers") {
            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.headerFn = { header.toByteArray() }
            delegate.footerFn = { footer.toByteArray() }

            delegate.performWrites(contents)

            assertThat(outputFiles).eachIndexed(4) { file, index ->
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
            }
        }
        val outputFiles by memoized(TEST) {
            directory.listFiles()
                    ?.map { it!! }
                    ?.sortedBy { it.name }
                    ?: emptyList()
        }

        it("should store a few short files without headers and footers") {
            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")

            delegate.performWrites(contents)

            assertThat(outputFiles).eachIndexed(4) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(contents[index])
                file.prop("name") { it.name }.isEqualTo("test$index.out")
                file.prop("parent") { it.parent }.isEqualTo(directory.path)
            }
        }

        it("should store a few short files with headers and footers") {
            val contents = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
            val header = "header1234567890"
            val footer = "footer1234567890"

            delegate.headerFn = { header.toByteArray() }
            delegate.footerFn = { footer.toByteArray() }

            delegate.performWrites(contents)

            assertThat(outputFiles).eachIndexed(4) { file, index ->
                file.prop("content") { it.readText() }.isEqualTo(header + contents[index] + footer)
                file.prop("name") { it.name }.isEqualTo("test$index.out")
                file.prop("parent") { it.parent }.isEqualTo(directory.path)
            }
        }
    }
})

private fun WriterDelegate<Unit>.performWrites(contents: List<String>) {
    contents.forEach {
        this.write(it.toByteArray())
        this.flush(null)
    }
    this.close()
}

