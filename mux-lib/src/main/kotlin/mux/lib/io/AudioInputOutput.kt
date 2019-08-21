package mux.lib.io

import mux.lib.TimeRangeProjectable
import mux.lib.file.Informable
import mux.lib.stream.MuxStream
import mux.lib.stream.Sample
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface StreamOutput {
    fun getInputStream(): InputStream
}

interface FixedOutput : StreamOutput {
    fun toByteArray(): ByteArray

    fun dataSize(): Int

}

interface AudioInput : Informable, MuxStream<Sample>, TimeRangeProjectable<AudioInput> {

    /** Amount of samples available */
    fun sampleCount(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long

    /**
     *  Gets the input as a sequence of samples.
     *
     *  @param sampleRate sample rate to use for generating sequence. If sample rate is different
     *          from the source iy should be resampled, or an exception should be thrown if it's not supported
     *  @throws UnsupportedOperationException if input doesn't support resampling
     **/
    override fun asSequence(sampleRate: Float): Sequence<Sample>
}
