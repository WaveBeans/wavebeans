package io.wavebeans.execution.distributed

import mu.KotlinLogging
import java.util.concurrent.Callable
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
            processBuilder.redirectError()
            processBuilder.redirectOutput()
        }
        val process = processBuilder.start()

        return try {
            val exitCode = process.waitFor()
            CommandResult(
                    exitCode,
                    process.inputStream.readBytes() + process.errorStream.readBytes()
            )
        } catch (e: InterruptedException) {
            log.debug { "[$id] Process $process destroying" }
            process.destroy()
            CommandResult(
                    -1,
                    process.inputStream.readBytes() + process.errorStream.readBytes()
            )
        }.also { log.debug { "[$id] Exit=${it.exitCode}, Output:\n${String(it.output)}" } }
    }
}