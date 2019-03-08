package mux.cli.scope

import mux.cli.command.Command
import mux.cli.command.PlayCommand

class AudioFileSelectRangeScope(val parent: AudioFileScope, val selection: Selection) : Scope {

    override fun prompt(): String = "${parent.prompt()}[${selection.start}:${selection.end}]"

    override fun commands(): Set<Command> {
        val descriptor = parent.samples().descriptor
        return setOf(
                PlayCommand(parent.samples(), selection.start.sampleIndex(descriptor), selection.end.sampleIndex(descriptor))
        )
    }

    override fun initialOutput(): String? {
        return null
    }

}