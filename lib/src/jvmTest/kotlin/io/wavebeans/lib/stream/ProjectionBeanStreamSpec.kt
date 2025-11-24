package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.lib.asInt
import io.wavebeans.lib.stream
import io.wavebeans.lib.TimeUnit

class ProjectionBeanStreamSpec : DescribeSpec({
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