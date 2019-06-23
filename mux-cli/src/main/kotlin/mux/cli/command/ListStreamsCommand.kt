package mux.cli.command

import mux.cli.Session
import java.util.concurrent.TimeUnit

class ListStreamsCommand(val session: Session) : InScopeCommand(
        "list",
        "Lists all available streams",
        { _, _ ->
            val count = session.streamsSequence().count()
            if (count == 0) {
                "No streams"
            } else {
                session.streamsSequence().joinToString(separator = "\n") { e ->

                    val length = String.format("%.2f sec", e.stream.length(session.outputDescriptor.sampleRate, TimeUnit.MILLISECONDS) / 1000.0)
                    val info = e.stream.info().entries.joinToString(separator = ", ") {
                        "${it.key}=${it.value}"
                    }
                    "[${e.name}] $length ($info)"
                }
            }
        }
)