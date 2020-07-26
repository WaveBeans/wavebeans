package io.wavebeans.http

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.ns
import io.wavebeans.lib.stream.SampleCountMeasurement
import java.io.InputStream
import java.util.*

data class BeanStreamElement(
        val offset: TimeMeasure,
        val value: Any
)

/**
 * Reads a sequence of elements as a new-line separated strings.
 */
abstract class BeanStreamReader(
        stream: BeanStream<*>,
        sampleRate: Float,
        offset: TimeMeasure
) : InputStream() {

    private val iterator = stream.asSequence(sampleRate).iterator()

    private val buffer = LinkedList<Byte>()

    private val timePerSample = 1e9 / sampleRate

    private var currentOffset = offset.ns()

    override fun read(): Int {
        if (buffer.size > 0) return buffer.remove().toInt()
        if (iterator.hasNext()) {
            val obj = iterator.next()

            val v = stringifyObj(BeanStreamElement(currentOffset.ns, obj))
            currentOffset += (SampleCountMeasurement.samplesInObject(obj) * timePerSample).toLong()

            buffer.addAll((v + "\n").toByteArray().toList())
            if (buffer.size > 0) return buffer.remove().toInt()
        }
        return -1
    }

    protected abstract fun stringifyObj(obj: BeanStreamElement): String
}