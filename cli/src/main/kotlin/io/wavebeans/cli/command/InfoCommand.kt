package io.wavebeans.cli.command

import mux.cli.Session
import mux.lib.stream.SampleStream

class InfoCommand(val session: Session, samples: SampleStream) : InScopeCommand(
        "info",
        "Outputs the file info. Usage: info",
        { _, _ ->
            TODO()
//            samples.info()
//                    .plus(mapOf("Length" to "" + samples.length(session.outputDescriptor.sampleRate) + "ms"))
//                    .entries
//                    .joinToString(separator = "\n") { "${it.key}: ${it.value}" }
        })