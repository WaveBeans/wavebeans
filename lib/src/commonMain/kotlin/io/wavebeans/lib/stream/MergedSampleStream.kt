package io.wavebeans.lib.stream

import io.wavebeans.lib.*

operator fun BeanStream<Sample>.minus(d: BeanStream<Sample>): BeanStream<Sample> = this.merge(with = d) { (x, y) -> x - y }

operator fun BeanStream<Sample>.plus(d: BeanStream<Sample>): BeanStream<Sample> = this.merge(with = d) { (x, y) -> x + y }

operator fun BeanStream<Sample>.times(d: BeanStream<Sample>): BeanStream<Sample> = this.merge(with = d) { (x, y) -> x * y }

operator fun BeanStream<Sample>.div(d: BeanStream<Sample>): BeanStream<Sample> = this.merge(with = d) { (x, y) -> if (y != null) x / y else ZeroSample }
