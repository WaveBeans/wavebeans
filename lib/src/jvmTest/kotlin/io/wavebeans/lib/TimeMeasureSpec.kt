package io.wavebeans.lib

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import io.wavebeans.lib.TimeUnit.*

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

    describe("Parsing") {
        it("should be 1234567890123456 nanoseconds") { assertThat(TimeMeasure.parse("1234567890123456ns")).isEqualTo(1234567890123456L.ns) }
        it("should be 1234567890123456 nanoseconds") { assertThat(TimeMeasure.parse("1234567890123456Ns")).isEqualTo(1234567890123456L.ns) }
        it("should be 1234567890123456 nanoseconds") { assertThat(TimeMeasure.parse("1234567890123456LNS")).isEqualTo(1234567890123456L.ns) }
        it("should be -1 microseconds") { assertThat(TimeMeasure.parse("-1us")).isEqualTo((-1).us) }
        it("should be -1 microseconds") { assertThat(TimeMeasure.parse("-1US")).isEqualTo((-1).us) }
        it("should be -1 microseconds") { assertThat(TimeMeasure.parse("-1LUs")).isEqualTo((-1).us) }
        it("should be 0 milliseconds") { assertThat(TimeMeasure.parse("0ms")).isEqualTo(0.ms) }
        it("should be 0 milliseconds") { assertThat(TimeMeasure.parse("0MS")).isEqualTo(0.ms) }
        it("should be 0 milliseconds") { assertThat(TimeMeasure.parse("0mS")).isEqualTo(0.ms) }
        it("should be -1e-6 seconds") { assertThat(TimeMeasure.parse("-1e-6s")).isEqualTo((-1e-6).s) }
        it("should be -1e-6 seconds") { assertThat(TimeMeasure.parse("-1e-6S")).isEqualTo((-1e-6).s) }
        it("should be 1.2e8 minutes") { assertThat(TimeMeasure.parse("1.2e8m")).isEqualTo((1.2e8).m) }
        it("should be 1.2e8 minutes") { assertThat(TimeMeasure.parse("1.2e8M")).isEqualTo((1.2e8).m) }
        it("should be 0 hours") { assertThat(TimeMeasure.parse("0.2h")).isEqualTo(0.h) }
        it("should be 0 hours") { assertThat(TimeMeasure.parse("0.2H")).isEqualTo(0.h) }
        it("should be 0 hours") { assertThat(TimeMeasure.parse("0.2fH")).isEqualTo(0.h) }
        it("should be 0 hours") { assertThat(TimeMeasure.parse("0fH")).isEqualTo(0.h) }
        it("should be 2 days") { assertThat(TimeMeasure.parse("2.2d")).isEqualTo(2.d) }
        it("should be 2 days") { assertThat(TimeMeasure.parse("2.2fd")).isEqualTo(2.d) }
        it("should be 2 days") { assertThat(TimeMeasure.parse("2.2D")).isEqualTo(2.d) }
        it("should not be parsed") {
            assertThat(catch { TimeMeasure.parse("2.2") })
                    .isNotNull().message().isNotNull().startsWith("Format invalid, should be:")
            assertThat(catch { TimeMeasure.parse("2") })
                    .isNotNull().message().isNotNull().startsWith("Format invalid, should be:")
            assertThat(catch { TimeMeasure.parse("") })
                    .isNotNull().message().isNotNull().startsWith("Format invalid, should be:")
            assertThat(catch { TimeMeasure.parse("-1f") })
                    .isNotNull().message().isNotNull().startsWith("Format invalid, should be:")
            assertThat(catch { TimeMeasure.parse("1megasecond") })
                    .isNotNull().message().isNotNull().startsWith("Format invalid, should be:")
        }
    }

    describe("As nanoseconds") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.ns()).isEqualTo(2 * 24 * 60 * 60 * 1000L * 1000L * 1000L) }
        it("should be 3.h") { assertThat(3.h.ns()).isEqualTo(3 * 60 * 60 * 1000L * 1000L * 1000L) }
        it("should be 123.m") { assertThat(123.m.ns()).isEqualTo(123 * 60 * 1000L * 1000L * 1000L) }
        it("should be 42.s") { assertThat(42.s.ns()).isEqualTo(42 * 1000L * 1000L * 1000L) }
        it("should be 42.ms") { assertThat(42.ms.ns()).isEqualTo(42L * 1000L * 1000L) }
        it("should be 2.us") { assertThat(2.us.ns()).isEqualTo(2L * 1000L) }
        it("should be 1.ns") { assertThat(1.ns.ns()).isEqualTo(1L) }
    }

    describe("As microseconds") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.us()).isEqualTo(2 * 24 * 60 * 60 * 1000L * 1000L) }
        it("should be 3.h") { assertThat(3.h.us()).isEqualTo(3 * 60 * 60 * 1000L * 1000L) }
        it("should be 123.m") { assertThat(123.m.us()).isEqualTo(123 * 60 * 1000L * 1000L) }
        it("should be 42.s") { assertThat(42.s.us()).isEqualTo(42 * 1000L * 1000L) }
        it("should be 42.ms") { assertThat(42.ms.us()).isEqualTo(42L * 1000L) }
        it("should be 2.us") { assertThat(2.us.us()).isEqualTo(2L) }
        it("should be 1.ns=0.us") { assertThat(1.ns.us()).isEqualTo(0L) }
    }

    describe("As milliseconds") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.ms()).isEqualTo(2 * 24 * 60 * 60 * 1000L) }
        it("should be 3.h") { assertThat(3.h.ms()).isEqualTo(3 * 60 * 60 * 1000L) }
        it("should be 123.m") { assertThat(123.m.ms()).isEqualTo(123 * 60 * 1000L) }
        it("should be 42.s") { assertThat(42.s.ms()).isEqualTo(42 * 1000L) }
        it("should be 42.ms") { assertThat(42.ms.ms()).isEqualTo(42L) }
        it("should be 2.us=0.ms") { assertThat(2.us.ms()).isEqualTo(0L) }
        it("should be 1.ns=0.ms") { assertThat(1.ns.ms()).isEqualTo(0L) }
    }

    describe("As seconds") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.s()).isEqualTo(2 * 24 * 60 * 60L) }
        it("should be 3.h") { assertThat(3.h.s()).isEqualTo(3 * 60 * 60L) }
        it("should be 123.m") { assertThat(123.m.s()).isEqualTo(123 * 60L) }
        it("should be 42.s") { assertThat(42.s.s()).isEqualTo(42L) }
        it("should be 42.ms=0.s") { assertThat(42.ms.s()).isEqualTo(0L) }
        it("should be 2.us=0.s") { assertThat(2.us.s()).isEqualTo(0L) }
        it("should be 1.ns=0.s") { assertThat(1.ns.s()).isEqualTo(0L) }
    }

    describe("As minutes") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.m()).isEqualTo(2 * 24 * 60L) }
        it("should be 3.h") { assertThat(3.h.m()).isEqualTo(3 * 60L) }
        it("should be 123.m") { assertThat(123.m.m()).isEqualTo(123L) }
        it("should be 42.s=0.m") { assertThat(42.s.m()).isEqualTo(0L) }
        it("should be 42.ms=0.m") { assertThat(42.ms.m()).isEqualTo(0L) }
        it("should be 2.us=0.m") { assertThat(2.us.m()).isEqualTo(0L) }
        it("should be 1.ns=0.m") { assertThat(1.ns.m()).isEqualTo(0L) }
    }

    describe("As hours") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.h()).isEqualTo(2 * 24L) }
        it("should be 3.h") { assertThat(3.h.h()).isEqualTo(3L) }
        it("should be 123.m=2.h") { assertThat(123.m.h()).isEqualTo(2L) }
        it("should be 42.s=0.h") { assertThat(42.s.h()).isEqualTo(0L) }
        it("should be 42.ms=0.h") { assertThat(42.ms.h()).isEqualTo(0L) }
        it("should be 2.us=0.h") { assertThat(2.us.h()).isEqualTo(0L) }
        it("should be 1.ns=0.h") { assertThat(1.ns.h()).isEqualTo(0L) }
    }

    describe("As days") {
        it("should be 2.5.d=2.d") { assertThat(2.5.d.d()).isEqualTo(2L) }
        it("should be 3.h=0.d") { assertThat(3.h.d()).isEqualTo(0L) }
        it("should be 123.m=0.d") { assertThat(123.m.d()).isEqualTo(0L) }
        it("should be 42.s=0.d") { assertThat(42.s.d()).isEqualTo(0L) }
        it("should be 42.ms=0.d") { assertThat(42.ms.d()).isEqualTo(0L) }
        it("should be 2.us=0.d") { assertThat(2.us.d()).isEqualTo(0L) }
        it("should be 1.ns=0.d") { assertThat(1.ns.d()).isEqualTo(0L) }
    }
})