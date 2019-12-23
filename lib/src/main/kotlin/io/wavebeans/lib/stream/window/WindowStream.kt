package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.jvm.jvmName

/**
 * Creates a [BeanStream] of [Window] of type [Sample].
 *
 * @param size the size of the window. Must be more than 1.
 */
fun BeanStream<Sample>.window(size: Int): BeanStream<Window<Sample>> =
        WindowStream(this, WindowStreamParams(size, size) { ZeroSample })

/**
 * Creates a [BeanStream] of [Window] of type [Sample].
 *
 * @param size the size of the window. Must be more than 1.
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 */
fun BeanStream<Sample>.window(size: Int, step: Int): BeanStream<Window<Sample>> =
        WindowStream(this, WindowStreamParams(size, step) { ZeroSample })

/**
 * Creates a [BeanStream] of [Window] of specified type.
 *
 * @param size the size of the window. Must be more than 1.
 * @param zeroElFn function that creates zero element objects.
 */
fun <T : Any> BeanStream<T>.window(size: Int, zeroElFn: () -> T): BeanStream<Window<T>> =
        WindowStream(this, WindowStreamParams(size, size, zeroElFn))

/**
 * Creates a [BeanStream] of [Window] of specified type.
 *
 * @param size the size of the window. Must be more than 1.
 * @param step the step to use for a sliding window. Must be more or equal to 1.
 * @param zeroElFn function that creates zero element objects.
 */
fun <T : Any> BeanStream<T>.window(size: Int, step: Int, zeroElFn: () -> T): BeanStream<Window<T>> =
        WindowStream(this, WindowStreamParams(size, step, zeroElFn))


object WindowStreamParamsSerializer : KSerializer<WindowStreamParams<*>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("FunctionMergedStreamParams") {
        init {
            addElement("windowSize")
            addElement("step")
            addElement("zeroElFn")
        }
    }

    override fun deserialize(decoder: Decoder): WindowStreamParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var windowSize: Int? = null
        var step: Int? = null
        var funcClazzName: String? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> windowSize = dec.decodeIntElement(descriptor, i)
                1 -> step = dec.decodeIntElement(descriptor, i)
                2 -> funcClazzName = dec.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST") val funcByName = Class.forName(funcClazzName).newInstance() as () -> Any
        return WindowStreamParams(windowSize!!, step!!, funcByName)
    }

    override fun serialize(encoder: Encoder, obj: WindowStreamParams<*>) {
        val funcName = obj.zeroElFn::class.jvmName
        val structure = encoder.beginStructure(descriptor)
        structure.encodeIntElement(descriptor, 0, obj.windowSize)
        structure.encodeIntElement(descriptor, 1, obj.step)
        structure.encodeStringElement(descriptor, 2, funcName)
        structure.endStructure(descriptor)
    }

}


/**
 * Parameters for [WindowStream].
 *
 * @param windowSize the size of the window. Must be more than 1.
 * @param step the size of the step to move window forward. For a fixed window should be the same as [windowSize]. Must be more or equal to 1.
 */
@Serializable(with = WindowStreamParamsSerializer::class)
class WindowStreamParams<T : Any>(
        val windowSize: Int,
        val step: Int,
        val zeroElFn: () -> T
) : BeanParams() {
    init {
        require(step >= 1) { "Step should be more or equal to 1" }
        require(windowSize > 1) { "Window size should be more than 1" }
    }
}


/**
 * The class provides windowed access to the underlying stream. The type of the stream can be Fixed and Sliding.
 * The difference is only how you define the [WindowStreamParams.step] -- if it's the same as [WindowStreamParams.windowSize]
 * it is considered to be Fixed, otherwise Sliding.
 *
 * Window stream supports a lot of operations, however make sure the window attributes are the same, otherwise you'll
 * get the runtime exception.
 *
 * This implementation works pretty much with any sampled medium, and uses generic [BeanStream] as source of data.
 *
 * @param T the type of the medium. Must be non-nullable type.
 */
class WindowStream<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: WindowStreamParams<T>
) : BeanStream<Window<T>>, AlterBean<T, Window<T>>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<Window<T>> {
        return input.asSequence(sampleRate)
                .windowed(
                        size = parameters.windowSize,
                        step = parameters.step,
                        partialWindows = true
                )
                .map { Window(parameters.windowSize, parameters.step, it, parameters.zeroElFn) }

    }

    override fun inputs(): List<AnyBean> = listOf(input)
}