package mux.lib.stream

@Suppress("NOTHING_TO_INLINE")
inline fun Sequence<Sample>.zeroPadLeft(count: Int): Sequence<Sample> = zeroPadding(count).plus(this)

@Suppress("NOTHING_TO_INLINE")
inline fun Sequence<Sample>.zeroPadRight(count: Int): Sequence<Sample> = this.plus(zeroPadding(count))

@Suppress("NOTHING_TO_INLINE")
inline fun zeroPadding(count: Int): Sequence<Sample> = (0 until count).asSequence()
        .map { ZeroSample }


