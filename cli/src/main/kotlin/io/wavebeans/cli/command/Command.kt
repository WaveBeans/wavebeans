package io.wavebeans.cli.command

import io.wavebeans.cli.Session
import io.wavebeans.cli.scope.AudioStreamScope
import io.wavebeans.cli.scope.Scope
import io.wavebeans.lib.stream.SampleStream

interface Command {
    fun name(): String

    fun description(): String
}

abstract class NewScopeCommand(
        private val name: String,
        private val description: String,
        private val scopeCreator: (String?) -> Scope
) : Command {

    override fun name(): String = name

    override fun description(): String = description

    fun newScope(args: String?) = scopeCreator(args)
}

abstract class InScopeCommand(
        val name: String,
        val description: String,
        val scopeModifier: (Scope, String?) -> String?
) : Command {

    override fun name(): String = name

    override fun description(): String = description

    fun run(caller: Scope, args: String?): String? = scopeModifier(caller, args)
}

abstract class AbstractNewSamplesScopeCommand(
        val session: Session,
        name: String,
        description: String,
        val samplesCreator: (String?) -> Pair<String, SampleStream>

) : NewScopeCommand(
        name,
        description,
        { input ->
            val (streamName, sampleStream) = samplesCreator(input)
            session.registerSampleStream(streamName, sampleStream)
            AudioStreamScope(session, streamName, sampleStream)
        }
)

abstract class CommandException(message: String) : Exception(message)

class ArgumentMissingException(message: String) : CommandException(message)

class ArgumentWrongException(message: String) : CommandException(message)
