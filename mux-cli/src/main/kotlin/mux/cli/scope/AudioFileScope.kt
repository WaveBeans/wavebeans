package mux.cli.scope

import mux.cli.command.*
import mux.lib.stream.SampleStream


class AudioFileScope(private val filename: String, private val samples: SampleStream) : Scope {

    fun samples() = samples

    override fun initialOutput(): String? = ""//samples.descriptor.toString()

    override fun prompt(): String = "`$filename`"

    override fun commands(): Set<Command> {
        return setOf(
                InfoCommand(samples),
                PlayCommand(samples, null, null),
                SaveFileCommand(samples, null, null),
                DownsampleCommand(samples),
                SelectCommand(this)
        )
    }
}

