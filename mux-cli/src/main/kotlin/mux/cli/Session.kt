package mux.cli

import mux.cli.command.ArgumentWrongException
import mux.lib.BitDepth
import mux.lib.stream.SampleStream

data class OutputDescriptor(
        val sampleRate: Float,
        val bitDepth: BitDepth
)

class Session {

    private val scopes = HashMap<String, SampleStream>()

    val outputDescriptor = OutputDescriptor(44100.0f, BitDepth.BIT_16)

    fun registerSampleStream(streamName: String, samples: SampleStream) {
        if (scopes.containsKey(streamName)) throw ArgumentWrongException("$streamName is already exists. Drop it first")
        scopes[streamName] = samples
    }

    fun streamByName(streamName: String): SampleStream? = scopes[streamName]



}