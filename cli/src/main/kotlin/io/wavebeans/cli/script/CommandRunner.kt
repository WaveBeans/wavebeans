package io.wavebeans.cli.script

import mu.KotlinLogging
import java.util.concurrent.Callable
import kotlin.random.Random

data class CommandResult(
        val exitCode: Int,
        val output: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandResult

        if (exitCode != other.exitCode) return false
        if (!output.contentEquals(other.output)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exitCode
        result = 31 * result + output.contentHashCode()
        return result
    }
}

class CommandRunner(vararg commands: String) : Callable<CommandResult> {

    private val cmds = commands.toList()

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun call(): CommandResult {
        val id = Random.nextInt(0xFFFF, Int.MAX_VALUE).toString(16)
        log.debug { "[$id] Running command: ${cmds.joinToString(" ")}" }

        val process = ProcessBuilder(*cmds.toTypedArray())
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