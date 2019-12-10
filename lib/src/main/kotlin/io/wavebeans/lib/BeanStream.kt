package io.wavebeans.lib

interface BeanStream<T : Any> : Bean<T> {

    /**
     *  Gets the input as a sequence of samples.
     *
     *  @param sampleRate sample rate to use for generating sequence. If sample rate is different
     *          from the source iy should be resampled, or an exception should be thrown if it's not supported
     *  @throws UnsupportedOperationException if input doesn't support resampling
     **/
    fun asSequence(sampleRate: Float): Sequence<T>
}