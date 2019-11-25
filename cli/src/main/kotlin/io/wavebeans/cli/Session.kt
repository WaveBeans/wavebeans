package io.wavebeans.cli

import mux.cli.command.ArgumentWrongException
import mux.lib.BitDepth
import mux.lib.stream.SampleStream

data class OutputDescriptor(
        val sampleRate: Float,
        val bitDepth: BitDepth
)

data class StreamDescriptor(
        val name: String,
        val stream: SampleStream
)

class Session(
        val outputDescriptor: OutputDescriptor = OutputDescriptor(44100.0f, BitDepth.BIT_16)
) {

    private val scopes = HashMap<String, SampleStream>()

    fun registerSampleStream(streamName: String, samples: SampleStream) {
        if (scopes.containsKey(streamName)) throw ArgumentWrongException("$streamName is already exists. Drop it first")
        scopes[streamName] = samples
    }

    fun streamByName(streamName: String): SampleStream? = scopes[streamName]

    fun streamsSequence(): Sequence<StreamDescriptor> = scopes.asSequence().map { StreamDescriptor(it.key, it.value) }

}