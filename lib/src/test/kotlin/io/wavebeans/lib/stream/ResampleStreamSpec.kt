package io.wavebeans.lib.stream

import assertk.assertThat
import io.wavebeans.lib.io.input
import io.wavebeans.lib.isListOf
import io.wavebeans.tests.toList
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ResampleStreamSpec : Spek({
    describe("Resampling the input to match the output") {

        it("should upsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }.resample()

            assertThat(resampled.toList(2000.0f)).isListOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4)
        }

        it("should downsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }.resample(reduceFn = { it.sum() })

            assertThat(resampled.toList(500.0f)).isListOf(1, 5, 4)
        }
        it("should resample with custom function") {
            fun resample(a: ResamplingArgument<Int>): Sequence<Int> {
                require(a.factor == 0.5f)
                return a.inputSequence.map { listOf(it, -1) }.flatten()
            }

            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }.resample(resampleFn = ::resample)

            assertThat(resampled.toList(500.0f)).isListOf(0, -1, 1, -1, 2, -1, 3, -1, 4, -1)
        }
    }

    describe("Resampling the input to reprocess and then to match the output") {
        it("should upsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }
                    .resample(to = 2000.0f)
                    .map { it * 2 }
                    .resample()

            assertThat(resampled.toList(4000.0f)).isListOf(0, 0, 0, 0, 2, 2, 2, 2, 4, 4, 4, 4, 6, 6, 6, 6, 8, 8, 8, 8)
        }

        it("should downsample") {
            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }
                    .resample(to = 500.0f, reduceFn = { it.sum() })
                    .map { it * 2 }
                    .resample(reduceFn = { it.sum() })

            assertThat(resampled.toList(250.0f)).isListOf(12, 8)
        }

        it("should resample with custom function") {
            fun resample(a: ResamplingArgument<Int>): Sequence<Int> {
                require(a.factor == 2.5f || a.factor == 0.1f)
                return if (a.factor == 2.5f)
                    a.inputSequence.map { listOf(it, -1) }.flatten()
                else
                    a.inputSequence.map { listOf(it, -3) }.flatten()
            }

            val resampled = input(1000.0f) { (i, fs) ->
                require(fs == 1000.0f) { "Non 1000Hz sample rate is not supported" }
                if (i < 5) i.toInt() else null
            }
                    .resample(to = 2500.0f, resampleFn = ::resample)
                    .map { it * 2 }
                    .resample(resampleFn = ::resample)

            assertThat(resampled.toList(250.0f)).isListOf(
                    0, -3, -2, -3,
                    2, -3, -2, -3,
                    4, -3, -2, -3,
                    6, -3, -2, -3,
                    8, -3, -2, -3,
            )
        }
    }
})
