package mux.cli.scope

import mux.cli.Session
import mux.cli.command.*
import mux.lib.stream.SampleStream


open class AudioStreamScope(
        val session: Session,
        val streamName: String,
        val samples: SampleStream
) : Scope {

    override fun initialOutput(): String? = null

    override fun prompt(): String = "`$streamName`"

    override fun commands(): Set<Command> {
        return setOf(
                InfoCommand(session, samples),
                PlayCommand(session, samples, null, null),
                SaveFileCommand(session, streamName, null, null),
                SelectCommand(session,this),
                MixStreamCommand(session, streamName),
                ListStreamsCommand(session)
        )
    }
}

