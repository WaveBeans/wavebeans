package io.wavebeans.lib.stream.window

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.stream.merge

operator fun BeanStream<Window<Sample>>.minus(d: BeanStream<Window<Sample>>): BeanStream<Window<Sample>> =
        this.merge(d) { x, y -> merge(x, y) { a, b -> a - b } }

operator fun BeanStream<Window<Sample>>.plus(d: BeanStream<Window<Sample>>): BeanStream<Window<Sample>> =
        this.merge(d) { x, y -> merge(x, y) { a, b -> a + b } }

operator fun BeanStream<Window<Sample>>.times(d: BeanStream<Window<Sample>>): BeanStream<Window<Sample>> =
        this.merge(d) { x, y -> merge(x, y) { a, b -> a * b } }

operator fun BeanStream<Window<Sample>>.div(d: BeanStream<Window<Sample>>): BeanStream<Window<Sample>> =
        this.merge(d) { x, y -> merge(x, y) { a, b -> a / b } }

private fun merge(x: Window<Sample>?, y: Window<Sample>?, fn: (Sample, Sample) -> Sample): Window<Sample> =
        x?.merge(y, fn)
                ?: if (y != null) {
                    Window.ofSamples(y.size, y.step, (0 until y.size).map { ZeroSample })
                            .merge(y, fn)
                } else {
                    throw IllegalStateException("Empty window is not defined")
                }
