package io.wavebeans.execution.distributed

import mu.KotlinLogging
import java.util.concurrent.Callable
import kotlin.random.Random

class CommandRunner(vararg commands: String) : Callable<CommandResult> {

    private val cmds = commands.toList()

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun call(): CommandResult {
        val id = Random.nextInt(0xFFFF, Int.MAX_VALUE).toString(16)
        log.debug { "[$id] Running command: ${cmds.joinToString(" ")}" }

        val tempDir = createTempDir().also { it.deleteOnExit() }
        val process = ProcessBuilder(*cmds.toTypedArray())
                .directory(tempDir)
                .inheritIO()
                .start()

        return try {
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                CommandResult(
                        exitCode,
                        process.errorStream.readBytes()
                )
            } else {
                CommandResult(
                        exitCode,
                        process.inputStream.readBytes()
                )
            }
        } catch (e: InterruptedException) {
            log.debug { "[$id] Process $process destroying" }
            process.destroy()
            CommandResult(
                    -1,
                    process.inputStream.readBytes()
            )
        }.also { log.debug { "[$id] Exit=${it.exitCode}, Output:\n${String(it.output)}" } }
    }
}