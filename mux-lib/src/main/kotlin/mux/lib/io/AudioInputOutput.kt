package mux.lib.io

import mux.lib.BitDepth
import mux.lib.TimeRangeProjectable
import mux.lib.file.Informable
import mux.lib.stream.AudioSampleStream
import mux.lib.stream.Sample
import mux.lib.stream.SampleStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface AudioOutput {
    fun toByteArray(): ByteArray

    fun getInputStream(): InputStream

    fun dataSize(): Int

}

interface AudioInput : Informable, TimeRangeProjectable<AudioInput> {

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
    fun asSequence(sampleRate: Float): Sequence<Sample>
}
