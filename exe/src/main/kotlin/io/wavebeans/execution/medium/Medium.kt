package io.wavebeans.execution.medium

/**
 * [Medium] serializer.
 */
interface MediumSerializer {
    fun size(): Int

    fun writeTo(buf: ByteArray, at: Int)

    fun type(): String
}

/**
 * Base interface for transferable between pods objects that can be serialized if that is required.
 * Also transparently allows working with elements at index.
 *
 * Medium concatenate the bunch of values AKA partition and doesn't hold one element but the list of them.
 * It is being used by [io.wavebeans.execution.pod.AbstractPod] to wrap the values to transfer between pods.
 * It is used via [io.wavebeans.execution.podproxy.PodProxyIterator] that iterates over elements in the medium
 * and return them one by one. It is an optimization to avoid excessive transferring between pods.
 */
interface Medium {

    /**
     * Gets the medium serializer.
     *
     * @throws UnsupportedOperationException if that medium doesn't support the serialization. Mainly thrown for
     * debug purposes as by design that shouldn't happen in real world scenarios.
     *
     * @return [MediumSerializer] that can be used to serialize medium to [ByteArray]
     */
    fun serializer(): MediumSerializer

    /**
     * Extract element at specified index.
     *
     * @param at the 0-based index of the element to extract.
     *
     * @return the element at the specified index or `null` if there is no such.
     */
    fun extractElement(at: Int): Any?
}

/**
 * [Medium] builder out of list of objects
 */
interface MediumBuilder {

    /**
     * Creates a [Medium] out of the objects list
     */
    fun from(objects: List<Any>): Medium
}