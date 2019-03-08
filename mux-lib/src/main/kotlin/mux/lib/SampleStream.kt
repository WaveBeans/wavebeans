package mux.lib

abstract class SampleStream(
        val buffer: ByteArray,
        val descriptor: AudioFileDescriptor
) {
    fun toByteArray(): ByteArray = buffer
}

class SampleStream16(buffer: ByteArray, descriptor: AudioFileDescriptor) : SampleStream(buffer, descriptor)

