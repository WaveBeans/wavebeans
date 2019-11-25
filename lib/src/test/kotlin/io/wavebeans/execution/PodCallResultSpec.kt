package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import io.wavebeans.lib.Sample
import io.wavebeans.lib.createSampleArray
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.math.cos

object PodCallResultSpec : Spek({

    describe("Wrapping value") {

        fun result(value: Any?): PodCallResult = PodCallResult.wrap(
                Call.parseRequest("method?param=value"),
                value
        )

        describe("Wrapping Long") {

            val result = result(123L)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.long()).isEqualTo(123L) }

            describe("More Longs") {
                it("should wrap MAX_VALUE") { assertThat(result(Long.MAX_VALUE).long()).isEqualTo(Long.MAX_VALUE) }
                it("should wrap MIN_VALUE") { assertThat(result(Long.MIN_VALUE).long()).isEqualTo(Long.MIN_VALUE) }
                it("should wrap 0L") { assertThat(result(0L).long()).isEqualTo(0L) }
            }
        }

        describe("Wrapping Sample") {
            val result = result(sampleOf(1.0))

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.sample()).isEqualTo(sampleOf(1.0)) }
        }

        describe("Wrapping Unit") {
            val result = result(Unit)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
        }

        describe("Wrapping null") {
            val result = result(null)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
        }

        describe("Wrapping list of samples") {
            val sampleList = listOf(sampleOf(1), sampleOf(2))
            val result = result(sampleList)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.sampleList()).isEqualTo(sampleList) }

            describe("More sample lists") {
                it("should wrap 256+ lists") {
                    val longList = (0..256).map { sampleOf(it) }
                    assertThat(result(longList).sampleList()).isEqualTo(longList)
                }
                it("should wrap 65536+ lists") {
                    val longList = (0..65536).map { sampleOf(it) }
                    assertThat(result(longList).sampleList()).isEqualTo(longList)
                }
                it("should wrap lists with type of values in it") {
                    val longList = (0..128).map {
                        when (it % 5) {
                            0 -> sampleOf(-1.0)
                            1 -> sampleOf(1.0)
                            2 -> sampleOf(0.0)
                            3 -> sampleOf(0.5)
                            4 -> sampleOf(-0.5)
                            else -> throw IllegalStateException("unreachable")
                        }

                    }
                    assertThat(result(longList).sampleList()).isEqualTo(longList)
                }
            }
        }

        describe("Wrapping list of samples array") {

            describe("Int range") {
                val sampleArrayList = listOf(
                        createSampleArray(2) { sampleOf(it) },
                        createSampleArray(2) { sampleOf(it + 2) }
                )

                val result = result(sampleArrayList)
                it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") {
                    assertThat(result.sampleArrayList()).eachIndexed(2) { el, i ->
                        el.containsExactly(*sampleArrayList[i])
                    }
                }
            }

            describe("Edge cases") {
                it("should wrap lists with type of values in it") {
                    val longList = (0..127).map {
                        when (it % 5) {
                            0 -> createSampleArray(10) { sampleOf(-1.0) }
                            1 -> createSampleArray(10) { sampleOf(1.0) }
                            2 -> createSampleArray(10) { sampleOf(0.0) }
                            3 -> createSampleArray(10) { sampleOf(0.5) }
                            4 -> createSampleArray(10) { sampleOf(-0.5) }
                            else -> throw IllegalStateException("unreachable")
                        }

                    }
                    assertThat(result(longList).sampleArrayList()).eachIndexed(128) { el, i ->
                        el.containsExactly(*longList[i])
                    }
                }

                it("should hold empty array") {
                    val list = listOf(
                            createSampleArray(0) { sampleOf(-1.0) }
                    )

                    assertThat(result(list).sampleArrayList()).eachIndexed(1) { el, i ->
                        el.containsExactly(*list[i])
                    }

                }

                it("should allow to have lists with different lengths of arrays") {
                    val list = listOf(
                            createSampleArray(10) { sampleOf(-1.0) },
                            createSampleArray(11) { sampleOf(1.0) }
                    )
                    assertThat(result(list).sampleArrayList()).eachIndexed(2) { el, i ->
                        el.containsExactly(*list[i])
                    }
                }
            }

            describe("Double sequence -- sinosoid") {
                val sampleRate = 44100.0
                val freq = 440
                val amplitude = 0.5
                val sampleArrayLength = 10
                val sampleArrayList = listOf(
                        createSampleArray(sampleArrayLength) { x -> amplitude * cos(x / sampleRate * 2 * Math.PI * freq) },
                        createSampleArray(sampleArrayLength) { x -> amplitude * cos((x + sampleArrayLength) / sampleRate * 2 * Math.PI * freq) }
                )
                val result = result(sampleArrayList)

                it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") {
                    assertThat(result.sampleArrayList()).eachIndexed(2) { el, i ->
                        el.containsExactly(*sampleArrayList[i])
                    }
                }

            }
        }

        describe("Wrapping empty list of samples") {
            val sampleList = listOf<Sample>()
            val result = result(sampleList)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.sampleList()).isEqualTo(sampleList) }
        }
    }

    describe("Wrapping errors") {

        fun result(value: Throwable): PodCallResult = PodCallResult.wrap(
                Call.parseRequest("method?param=value"),
                value
        )

        describe("Wrapping Exception") {
            val result = result(IllegalStateException("test message"))

            assertThat(catch { result.throwIfError() })
                    .isNotNull()
                    .cause()
                    .isNotNull()
                    .isInstanceOf(IllegalStateException::class)
                    .hasMessage("test message")
        }

        describe("Wrapping Error") {
            val result = result(NotImplementedError("test message"))

            assertThat(catch { result.throwIfError() })
                    .isNotNull()
                    .cause()
                    .isNotNull()
                    .isInstanceOf(NotImplementedError::class)
                    .hasMessage("test message")
        }
    }
})