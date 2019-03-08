package mux.cli

import mux.lib.AudioFileDescriptor
import mux.lib.SampleStream
import mux.lib.WavFileReader
import java.io.File
import java.io.FileInputStream

class Session {

    private var hasAudioFile = false

    private lateinit var descriptor: AudioFileDescriptor

    private lateinit var samples: SampleStream

    fun openAudioFile(filepath: String) {
        val f = File(filepath)
        if (!f.exists()) {
            throw IllegalStateException("`$filepath` is not found.")
        }
        val (d, ss) = WavFileReader(FileInputStream(f)).read()
        samples = ss
        descriptor = d
        hasAudioFile = true
    }

    fun samples(): SampleStream {
        if (!hasAudioFile) throw IllegalStateException("You should open file first")
        return samples
    }

    fun descriptor(): AudioFileDescriptor {
        if (!hasAudioFile) throw IllegalStateException("You should open file first")
        return descriptor
    }
}