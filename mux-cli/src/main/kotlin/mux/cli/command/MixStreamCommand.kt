package mux.cli.scope

import mux.cli.Session
import mux.cli.command.AbstractNewSamplesScopeCommand
import mux.cli.command.ArgumentMissingException
import mux.cli.command.ArgumentWrongException
import mux.cli.command.Selection
import java.util.concurrent.TimeUnit

class MixStreamCommand(session: Session, val sourceStreamName: String) : AbstractNewSamplesScopeCommand(
        session,
        "mix",
        """
            Mixes two stream together. Creates a new stream out of it.
            Usage: mix <stream name> [<position, default 0>] [<selection start>..<selection end>]
        """.trimIndent(),
        { args ->
            if (args == null) throw ArgumentMissingException("You should specify mix parameters.")
            val a = args.split(" ")
            val streamName = if (a.isNotEmpty()) a[0].trim() else throw ArgumentMissingException("stream name is missing")
            val position = if (a.size > 1) a[1].trim().toIntOrNull()
                    ?: throw ArgumentWrongException("position is an integer number") else 0
            val selection = if (a.size > 2) Selection.parse(session.outputDescriptor.sampleRate, a[2]) else null
            val sourceStream = session.streamByName(sourceStreamName)
                    ?: throw ArgumentWrongException("Stream by name `$sourceStreamName` is not found")
            val stream = session.streamByName(streamName)
                    ?: throw ArgumentWrongException("Stream by name `$streamName` is not found")

            val mixedStream = sourceStream.mixStream(
                    position,
                    selection
                            ?.let {
                                val tu = TimeUnit.NANOSECONDS
                                stream.rangeProjection(it.start.time(tu), it.end.time(tu), tu)
                            }
                            ?: stream
            )

            Pair("$sourceStreamName+$streamName", mixedStream)
        }
)