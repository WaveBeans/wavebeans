package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.asInt
import io.wavebeans.lib.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object ProjectionBeanStream : Spek({
    describe("Range with open end") {

        describe("Taking everything after 1s from 0..9") {
            val a = (0..9).stream()
                    .rangeProjection(1000, timeUnit = TimeUnit.MILLISECONDS)
                    .asSequence(2.0f)
                    .take(8)
                    .map { it.asInt() }
                    .toList()
            it("should be 2..9") { assertThat(a).isEqualTo((2..9).toList()) }
        }
    }
})