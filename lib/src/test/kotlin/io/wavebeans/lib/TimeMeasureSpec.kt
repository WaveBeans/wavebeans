package io.wavebeans.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.*

object TimeMeasureSpec : Spek({
    describe("Instantiating") {
        it("should be 1 nanoseconds") { assertThat(1.ns).isEqualTo(TimeMeasure(1L, NANOSECONDS)) }
        it("should be 2 * 10^3 microseconds") { assertThat(2e3.us).isEqualTo(TimeMeasure(2000L, MICROSECONDS)) }
        it("should be 4 milliseconds") { assertThat((1 + 3).ms).isEqualTo(TimeMeasure(4L, MILLISECONDS)) }
        it("should be 2 seconds") { assertThat(2.1.s).isEqualTo(TimeMeasure(2L, SECONDS)) }
        it("should be 3 minutes") { assertThat(3L.m).isEqualTo(TimeMeasure(3L, MINUTES)) }
        it("should be 6 hours") { assertThat(6.h).isEqualTo(TimeMeasure(6L, HOURS)) }
        it("should be 1000 days") { assertThat(1_000.d).isEqualTo(TimeMeasure(1000L, DAYS)) }
    }

    describe("Arithmetic operation") {
        it("should be 2.1 * 10^9 nanoseconds") { assertThat(2.s + 100.ms).isEqualTo(TimeMeasure(2_100_000_000, NANOSECONDS)) }
        it("should be -10^9 nanoseconds") { assertThat(119.s - 2.m).isEqualTo(TimeMeasure(-1_000_000_000, NANOSECONDS)) }
    }

    describe("Comparing") {
        it("should be true") { assertThat(1.s < 1200.ms).isTrue() }
        it("should be true") { assertThat(5.d == (5 * 86400).s).isTrue() }
        it("should be false") { assertThat(4.s >= 10e6.us).isFalse() }
    }
})