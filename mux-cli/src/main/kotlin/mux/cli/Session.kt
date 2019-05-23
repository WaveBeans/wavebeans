package mux.cli

import mux.cli.command.ArgumentWrongException
import mux.lib.stream.SampleStream

class Session {

    private val scopes = HashMap<String, SampleStream>()

    fun registerSampleStream(streamName: String, samples: SampleStream) {
        if (scopes.containsKey(streamName)) throw ArgumentWrongException("$streamName is already exists. Drop it first")
        scopes[streamName] = samples
    }

    fun streamByName(streamName: String): SampleStream? = scopes[streamName]

}