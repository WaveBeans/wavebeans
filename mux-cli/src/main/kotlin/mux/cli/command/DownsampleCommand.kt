package mux.cli.command

import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.lib.stream.SampleStream

class DownsampleCommand(
        val session: Session,
        val samples: SampleStream
) : NewScopeCommand("downSample", """
            Down sampling the current file.
            Usage: downSample <ratio>
            """.trimIndent(),
        { args ->

            val ratio = (args
                    ?: throw ArgumentMissingException("You need to define ratio, i.e. 2"))
                    .toIntOrNull()
                    ?: throw ArgumentWrongException("You need to define ratio in integer format, i.e. 2")


            AudioStreamScope(session, "downsampled", samples.downSample(ratio))
        })