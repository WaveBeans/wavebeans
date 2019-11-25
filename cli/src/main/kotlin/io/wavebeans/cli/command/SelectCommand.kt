package io.wavebeans.cli.command

import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.cli.scope.AudioSubStreamScope

class SelectCommand(val session: Session, parentScope: AudioStreamScope) : NewScopeCommand(
        "select",
        """
                            Selects the range of the file.

                            Usage: select <sample start value><sample unit>..<sample end value><sample unit>
                            Examples:
                                `select 0..100` - selects first 100 samples excluding last.
                                `select 1s..3s` - selects samples in range which conforms to 1.000s <= sample time < 3.000s
                                `select 1.050s..3s` - selects samples in range which conforms to 1.050s <= sample time < 3.000s
                                `select 1.050s..3000ms` - selects samples in range which conforms to 1.050s <= sample time < 3.000s

                            Supported formats for sample values:
                                1. Sample #. You need specify just number without unit, i.e. 10, 50, 10000
                                2. Time in seconds. You
                        """.trimIndent(),
        { args ->
            if (args == null) throw ArgumentMissingException("you need to specify sample range")
            val selection = try {
                Selection.parse(session.outputDescriptor.sampleRate, args)
            } catch (e: SelectionParseException) {
                throw ArgumentWrongException(e.message ?: "")
            }
            val subStreamName = parentScope.streamName + "<$selection>"
            val newScope = AudioSubStreamScope(subStreamName, session, parentScope, selection)
            session.registerSampleStream(
                    subStreamName,
                    newScope.samples
            )
            newScope
        })