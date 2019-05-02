package mux.cli.scope

import mux.cli.command.Command
import mux.cli.command.PlayCommand
import mux.cli.command.SaveFileCommand

class AudioFileSelectRangeScope(val parent: AudioFileScope, val selection: Selection) : Scope {

    override fun prompt(): String = "${parent.prompt()}[${selection.start}:${selection.end}]"

    override fun commands(): Set<Command> {
        TODO()
//        val descriptor = parent.samples().descriptor
//        return setOf(
//                PlayCommand(parent.samples(), selection.start.sampleIndex(descriptor), selection.end.sampleIndex(descriptor)),
//                SaveFileCommand(parent.samples(), selection.start.sampleIndex(descriptor), selection.end.sampleIndex(descriptor))
//        )
    }

    override fun initialOutput(): String? {
        return null
    }

}