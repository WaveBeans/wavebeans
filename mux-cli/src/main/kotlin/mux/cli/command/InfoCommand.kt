package mux.cli.command

import mux.lib.stream.SampleStream

class InfoCommand(samples: SampleStream) : InScopeCommand(
        "info",
        "Outputs the file info. Usage: info",
        { _, _ ->
            samples.info().entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }
        })