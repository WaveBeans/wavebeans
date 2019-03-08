package mux.cli.scope

import mux.cli.Session
import mux.cli.command.*
import mux.lib.SampleStream
import java.io.File
import javax.sound.sampled.AudioSystem


class AudioFileScope(session: Session, val filename: String) : Scope {

    private var samples: SampleStream

    init {
        session.openAudioFile(filename)
        samples = session.samples()
    }

    fun samples() = samples

    override fun initialOutput(): String? = samples.descriptor.toString()

    override fun prompt(): String = "`$filename`"

    override fun commands(): Set<Command> {
        return setOf(
                InScopeCommand(
                        "info",
                        "Outputs the file info. Usage: info"
                ) { _, _ ->
                    samples.descriptor.toString()
                },
                PlayCommand(samples, null, null),
                NewScopeCommand(
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
                        """.trimIndent()
                ) { args ->
                    if (args == null) throw ArgumentMissingException("you need to specify sample range")
                    val selection = try {
                        Selection.parse(args)
                    } catch (e: SelectionParseException) {
                        throw ArgumentWrongException(e.message ?: "")
                    }
                    AudioFileSelectRangeScope(this, selection)
                }
        )
    }
}
