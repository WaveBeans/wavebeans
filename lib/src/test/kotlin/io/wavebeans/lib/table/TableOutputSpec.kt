package io.wavebeans.lib.table

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.nhaarman.mockitokotlin2.*
import io.wavebeans.lib.*
import io.wavebeans.lib.io.input
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import org.mockito.verification.VerificationMode
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit

object TableOutputSpec : Spek({
    describe("SampleArray content") {
        val driver by memoized { mock<TimeseriesTableDriver<SampleArray>>() }

        val params by memoized {
            TableOutputParams(
                    tableName = "test",
                    tableType = SampleArray::class,
                    maximumDataLength = 1.m,
                    tableDriverFactory = object : Fn<TableOutputParams<SampleArray>, TimeseriesTableDriver<SampleArray>>() {
                        override fun apply(argument: TableOutputParams<SampleArray>): TimeseriesTableDriver<SampleArray> {
                            return driver
                        }
                    },
                    automaticCleanupEnabled = true
            )
        }

        val output by memoized {
            TableOutput(
                    input { (i, _) -> if (i < 2000) 1e-10 * i else null }
                            .window(1024)
                            .map { sampleArrayOf(it) },
                    params
            )
        }

        val writer by memoized {
            output.writer(44100.0f)
        }

        it("should properly put correct values with according time markers") {

            fun d(range: IntRange) = sampleArrayOf(
                    seqStream().asSequence(44100.0f)
                            .drop(range.first)
                            .take(range.last - range.first + 1)
                            .toList()
            )

            fun ns(sampleIdx: Long) = (sampleIdx.toDouble() / 44100.0 * 1e9).ns

            assertThat(writer.write()).isTrue()
            verify(driver).put(eq(ns(0)), eq(d(0..1023)))

            assertThat(writer.write()).isTrue()
            verify(driver).put(eq(ns(1024)), eq(d(1024..1999)))

            assertThat(writer.write()).isFalse()
            verify(driver).finishStream()
        }
    }
})