package io.wavebeans.lib.stream.window

import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample

data class Window<T>(
        val size: Int,
        val step: Int,
        val elements: List<T>,
        val zeroEl: () -> T
) {

    init {
        require(size >= 1) { "Size should be more than 1" }
        require(step >= 1) { "Step should be more than 1" }
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