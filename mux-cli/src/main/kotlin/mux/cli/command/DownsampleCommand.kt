package mux.cli.command

import mux.cli.scope.AudioFileScope
import mux.lib.SampleStream

class DownsampleCommand(
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


            AudioFileScope("downsampled", samples.downSample(ratio))
        })