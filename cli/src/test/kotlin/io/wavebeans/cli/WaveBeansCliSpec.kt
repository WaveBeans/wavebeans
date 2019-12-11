package io.wavebeans.cli

import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.cli.WaveBeansCli.Companion.name
import io.wavebeans.cli.WaveBeansCli.Companion.options
import java.io.PrintWriter
import org.apache.commons.cli.DefaultParser
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayOutputStream
import java.io.File

object WaveBeansCliSpec : Spek({
    describe("Scripting") {
        describe("Short-living script") {
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute", "440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()",
                            "--verbose"
                    )),
                    printer = PrintWriter(out)
            )
            out.flush()
            out.close()

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readText()).isNotEmpty() }
            it("should output something to console") { assertThat(String(out.toByteArray())).isNotEmpty() }
        }

        describe("Short-living from file") {
            val scriptFile = File.createTempFile("test", "kts").also { it.deleteOnExit() }
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            scriptFile.writeBytes("440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()".toByteArray())
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute-file", scriptFile.absolutePath,
                            "--time"
                    )),
                    printer = PrintWriter(out)
            )
            out.flush()
            out.close()

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readText()).isNotEmpty() }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+s\\s*"))
            }
        }
    }
})