package io.wavebeans.lib.stream.window

import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample

data class Window<T: Any>(
        /**
         * The size of the window it was created with.
         */
        val size: Int,
        /**
         * The step the window is moving on each iteration. If the window is fixed, meaning windows has no intersection
         * while moving, it should have the same value as [size]. If it has value less than [size] that means consequent
         * windows will have shared elements. If that value is greater then [size], that means windows are not intersecting
         * but some of the value are dropped during iteration.
         */
        val step: Int,
        /**
         * The elements of this window. The amount of elements must be less or equal to [size]. If elements has less values
         * that size, missed elements will be replaced by call to [zeroEl] function.
         */
        val elements: List<T>,
        /**
         * If [elements] has not enough element during some operations, it'll be replace by zero elements generated
         * by this function.
         */
        val zeroEl: () -> T
) {

    init {
        require(size >= 1) { "Size should be more than 0" }
        require(step >= 1) { "Step should be more than 0" }
        require(elements.isNotEmpty()) { "Window should have at least 1 element" }
    }

    companion object {
        fun ofSamples(size: Int, step: Int, elements: List<Sample>) = Window(size, step, elements) { ZeroSample }
    }

    fun merge(other: Window<T>?, fn: (T, T) -> T): Window<T> {
        check(other == null || this.size == other.size && this.step == other.step) {
            "Can't merge with stream with different window size or step"
        }
        val thisElements = this.elements + (0 until size - this.elements.size).map { zeroEl() }
        val otherList = other?.elements ?: emptyList()
        val otherElements = otherList + (0 until size - otherList.size).map { zeroEl() }
        return Window(
                size,
                step,
                thisElements.zip(otherElements).map { fn(it.first, it.second) },
                zeroEl
        )
    }
}