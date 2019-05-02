package mux.lib.io

import mux.lib.stream.Sample
import java.io.InputStream

interface AudioOutput {
    fun toByteArray(): ByteArray

    fun getInputStream(): InputStream

    fun dataSize(): Int

}

interface AudioInput {
    /** Amount of samples available */
    fun size(): Int

    /** Gets a substream of the current stream. */
    fun subInput(skip: Int, length: Int): AudioInput

    /** Gets the input as a sequence of samples. */
    fun asSequence(): Sequence<Sample>
}
