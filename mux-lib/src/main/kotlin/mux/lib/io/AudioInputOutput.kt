package mux.lib.io

import mux.lib.TimeRangeProjectable
import mux.lib.file.Informable
import mux.lib.stream.Sample
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface AudioOutput {
    fun toByteArray(): ByteArray

    fun getInputStream(): InputStream

    fun dataSize(): Int

}

interface AudioInput : Informable, TimeRangeProjectable<AudioInput> {

    /** Amount of samples available */
    fun size(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long

    /**
     * Return the input that is subrange of current input. If the input is read into a memory or just it defines
     * the reader parameters depends on specific implementation.
     *
     * @param skip number of samples to skip from the beginning. O if do not skip.
     * @param length number of samples to read.
     **/
//    @Deprecated("to get rid of it")
//    fun subInput(skip: Int, length: Int): AudioInput

    /**
     *  Gets the input as a sequence of samples.
     *
     *  @param sampleRate sample rate to use for generating sequence. If sample rate is different
     *          from the source iy should be resampled, or an exception should be thrown if it's not supported
     *  @throws UnsupportedOperationException if input doesn't support resampling
     **/
    fun asSequence(sampleRate: Float): Sequence<Sample>
}
