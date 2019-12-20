package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.map

operator fun BeanStream<Window<Sample>>.minus(d: Number): BeanStream<Window<Sample>> =
        this.map { window -> Window.ofSamples(window.size, window.step, window.elements.map { it - d.toDouble() }) }

operator fun BeanStream<Window<Sample>>.plus(d: Number): BeanStream<Window<Sample>> =
        this.map { window -> Window.ofSamples(window.size, window.step, window.elements.map { it + d.toDouble() }) }

operator fun BeanStream<Window<Sample>>.times(d: Number): BeanStream<Window<Sample>> =
        this.map { window -> Window.ofSamples(window.size, window.step, window.elements.map { it * d.toDouble() }) }

operator fun BeanStream<Window<Sample>>.div(d: Number): BeanStream<Window<Sample>> =
        this.map { window -> Window.ofSamples(window.size, window.step, window.elements.map { it / d.toDouble() }) }
