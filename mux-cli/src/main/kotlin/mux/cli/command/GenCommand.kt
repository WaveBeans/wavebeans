package mux.cli.command

import mux.cli.Session
import mux.lib.io.SineGeneratedInput
import mux.lib.stream.SampleStream
import mux.lib.stream.sampleStream
import java.util.*

enum class Generator(val type: String, val streamBuilder: () -> GeneratedStreamBuilder) {
    SINE("sine", { SineGeneratedStreamBuilder() })
}

private val availableGenTypes = Generator.values().joinToString(separator = ", ") { it.type }
private val generatorDesc = Generator.values()
        .joinToString(separator = "\n") {
            it.type + ":\n" + it.streamBuilder()
                    .getSchema()
                    .joinToString(separator = ", ") { p -> "${p.name}: ${p.description} (required:${(!p.isOptional)})" }
        }


class GenCommand(session: Session) : AbstractNewSamplesScopeCommand(
        session,
        "gen",
        """
            Generates sines of specified frequency.
            Usage: gen <generator type> <samplesCount in seconds, i.e. 1.0>
                   [d|depth=bit depth (8, 16, 24 or 32), default 16 bit] [generator parameters pairs like `parameter=value`...]

           Types of generators:
        """.trimIndent() + "\n" + generatorDesc,
        { input ->
            if (input == null) throw ArgumentMissingException("At least sine frequency should be specified")

            val options = input.split(" ")

            if (options.isEmpty()) throw ArgumentMissingException("At least sine frequency should be specified")
            val type = Generator.values()
                    .firstOrNull { it.type == options[0].trim().toLowerCase() }
                    ?: throw ArgumentWrongException("SampleStreamGenerator type is not recognized. Available: $availableGenTypes")
            val parameters = HashMap<String, String>()
            if (options.size > 2) {
                options.subList(2, options.size)
                        .map { it.split("=", limit = 2) }
                        .forEach { parameters[it[0]] = if (it.size > 1) it[1] else "" }
            }
            val bitDepth = (parameters["d"] ?: parameters["depth"])?.toIntOrNull() ?: 16

            if (options.size < 2) throw ArgumentMissingException("Length should be specified")
            val length = options[1].trim().toDoubleOrNull()
                    ?.also { if (it <= 0) throw ArgumentWrongException("Length should be more than zero") }
                    ?: throw ArgumentWrongException("Length should be float number")

            val stream = type.streamBuilder().build(length, bitDepth, parameters)
            Pair(type.type, stream)
        })


interface GeneratedStreamBuilder {

    data class ParameterSchema(
            val name: List<String>,
            val description: String,
            val isOptional: Boolean
    )

    fun getSchema(): List<ParameterSchema>

    fun build(length: Double, bitDepth: Int, input: Map<String, String>): SampleStream
}

class SineGeneratedStreamBuilder : GeneratedStreamBuilder {
    override fun getSchema(): List<GeneratedStreamBuilder.ParameterSchema> {
        return listOf(
                GeneratedStreamBuilder.ParameterSchema(
                        listOf("f", "frequency"),
                        "Frequency of the sinusoid to generate, double number greater than 0",
                        false
                ),
                GeneratedStreamBuilder.ParameterSchema(
                        listOf("a", "amplitude"),
                        "Amplitude of resulted sinusoid, double number greater than 0",
                        false
                )
        )
    }

    override fun build(length: Double, bitDepth: Int, input: Map<String, String>): SampleStream {
        val frequency = (input["f"] ?: input["frequency"])
                ?.toDoubleOrNull()
                ?.also { if (it < 0) throw ArgumentWrongException("frequency should be greater than 0") }
                ?: throw ArgumentMissingException("frequency should be specified")
        val amplitude = (input["a"] ?: input["amplitude"])
                ?.toDoubleOrNull()
                ?.also { if (it < 0) throw ArgumentWrongException("amplitude should be greater than 0") }
                ?: throw ArgumentMissingException("amplitude should be specified")
        return SineGeneratedInput(frequency, amplitude, length).sampleStream()
    }
}
