package io.wavebeans.tests

import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class CommandRunner(vararg commands: String) {

    private val cmds = commands.toList()

    companion object {
        private val log = KotlinLogging.logger { }
    }

    fun run(inheritIO: Boolean = true): CommandResult {
        val id = Random.nextInt(0xFFFF, Int.MAX_VALUE).toString(16)
        log.debug { "[$id] Running command: ${cmds.joinToString(" ")}" }

        val tempDir = createTempDir().also { it.deleteOnExit() }
        val processBuilder = ProcessBuilder(*cmds.toTypedArray())
                .directory(tempDir)

        if (inheritIO) {
            processBuilder.inheritIO()
        } else {
            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE)
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)
        }
        val process = processBuilder.start()

        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        return try {
            while (!process.waitFor(1000, TimeUnit.MILLISECONDS)) {
                output.write(process.inputStream.readBytes())
                error.write(process.errorStream.readBytes())
            }
            output.write(process.inputStream.readBytes())
            error.write(process.errorStream.readBytes())
            val exitCode = process.exitValue()
            CommandResult(
                    exitCode,
                    output.toByteArray() + error.toByteArray()
            )
        } catch (e: InterruptedException) {
            output.write(process.inputStream.readBytes())
            error.write(process.errorStream.readBytes())
            log.debug { "[$id] Process $process destroying" }
            process.destroy()
            CommandResult(
                    -1,
                    output.toByteArray() + error.toByteArray()
            )
        }.also { log.debug { "[$id] Exit=${it.exitCode}, Output:\n${String(it.output)}" } }
    }
}