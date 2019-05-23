package mux.cli.scope

import mux.cli.Session
import mux.cli.command.*
import mux.lib.stream.SampleStream


class AudioStreamScope(
        private val session: Session,
        private val streamName: String,
        private val samples: SampleStream
) : Scope {

    fun samples() = samples

    override fun initialOutput(): String? = ""//samples.descriptor.toString()

    override fun prompt(): String = "`$streamName`"

    override fun commands(): Set<Command> {
        return setOf(
                InfoCommand(samples),
                PlayCommand(samples, null, null),
                SaveFileCommand(session, streamName, null, null),
                DownsampleCommand(session, samples),
                SelectCommand(this),
                MixStreamCommand(session, streamName)
        )
    }
}

