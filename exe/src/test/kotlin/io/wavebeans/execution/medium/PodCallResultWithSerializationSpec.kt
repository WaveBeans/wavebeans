package io.wavebeans.execution.medium

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.execution.Call
import io.wavebeans.lib.Sample
import io.wavebeans.lib.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.xdescribe

object PodCallResultWithSerializationSpec : Spek({

    xdescribe("Wrapping value") {

        fun result(value: Any?): SerializazingPodCallResult = SerializingPodCallResultBuilder().ok(
                Call.parseRequest("method?param=value"),
                value
        ) as SerializazingPodCallResult

        describe("Wrapping Long") {

            val result = result(123L)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.decodeLong()).isEqualTo(123L) }

            describe("More Longs") {
                it("should wrap MAX_VALUE") { assertThat(result(Long.MAX_VALUE).decodeLong()).isEqualTo(Long.MAX_VALUE) }
                it("should wrap MIN_VALUE") { assertThat(result(Long.MIN_VALUE).decodeLong()).isEqualTo(Long.MIN_VALUE) }
                it("should wrap 0L") { assertThat(result(0L).decodeLong()).isEqualTo(0L) }
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


        describe("Wrapping empty list of samples") {
            val sampleList = listOf<Sample>()
            val result = result(sampleList)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.sampleList()).isEqualTo(sampleList) }
        }

        describe("Wrapping List of WindowSampleArray") {
            TODO()
//            val obj = listOf(
//                    createWindowSampleArray(10, 10, 2) { i -> Window.ofSamples(10, 10, (0..9).map { sampleOf(it * i) }) },
//                    createWindowSampleArray(15, 5, 2) { i -> Window.ofSamples(15, 5, (0..9).map { sampleOf(it * i) }) }
//            )
//            val result = result(obj)
//
//            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
//            it("should have empty exception") { assertThat(result.exception).isNull() }
//            it("should return valid value") {
//                assertThat(result.nullableWindowSampleArrayList())
//                        .isNotNull()
//                        .all {
//                            prop("window @ [0,0]") { it[0].sampleArray[0] }.isEqualTo(obj[0].sampleArray[0])
//                            prop("window @ [0,1]") { it[0].sampleArray[1] }.isEqualTo(obj[0].sampleArray[1])
//                            prop("window @ [1,0]") { it[1].sampleArray[0] }.isEqualTo(obj[1].sampleArray[0])
//                            prop("window @ [1,1]") { it[1].sampleArray[1] }.isEqualTo(obj[1].sampleArray[1])
//                        }
//            }
        }

        describe("Wrapping List<FftSampleArray>") {
            TODO()
//            val obj = listOf(
//                    createFftSampleArray(2) { i -> FftSample(i.toLong(), i, i, i * 123.0f, listOf(i.r, i.i)) },
//                    createFftSampleArray(2) { i -> FftSample(i.toLong(), i, i, i * 123.0f, listOf(i.r, i.i)) }
//            )
//            val result = result(obj)
//
//            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
//            it("should have empty exception") { assertThat(result.exception).isNull() }
//            it("should return valid value") {
//                assertThat(result.nullableFftSampleArrayList())
//                        .isNotNull()
//                        .all {
//                            prop("fft @ [0,0]") { it[0][0] }.isEqualTo(obj[0][0])
//                            prop("fft @ [0,1]") { it[0][1] }.isEqualTo(obj[0][1])
//                            prop("fft @ [1,0]") { it[1][0] }.isEqualTo(obj[1][0])
//                            prop("fft @ [1,1]") { it[1][1] }.isEqualTo(obj[1][1])
//                        }
//            }
        }
        describe("Wrapping List<Array<DoubleArray>>") {
            val obj = listOf(
                    Array(2) { i -> DoubleArray(i) { it.toDouble() + 123.0 } },
                    Array(2) { i -> DoubleArray(i) { it.toDouble() - 345.0 } }
            )
            val result = result(obj)

            it("should have non empty byteArray") { assertThat(result.byteArray).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") {
                assertThat(result.nullableDoubleArrayArrayList())
                        .isNotNull()
                        .all {
                            prop("doubleArray @ [0,0]") { it[0][0] }.isEqualTo(obj[0][0])
                            prop("doubleArray @ [0,1]") { it[0][1] }.isEqualTo(obj[0][1])
                            prop("doubleArray @ [1,0]") { it[1][0] }.isEqualTo(obj[1][0])
                            prop("doubleArray @ [1,1]") { it[1][1] }.isEqualTo(obj[1][1])
                        }
            }
        }
    }

    xdescribe("Wrapping errors") {

        fun result(value: Throwable): PodCallResult = PodCallResult.error(
                Call.parseRequest("method?param=value"),
                value
        )

        describe("Wrapping Exception") {
            val result = result(IllegalStateException("test message"))

            assertThat(result.exception)
                    .isNotNull()
                    .isInstanceOf(IllegalStateException::class)
                    .hasMessage("test message")
        }

        describe("Wrapping Error") {
            val result = result(NotImplementedError("test message"))

            assertThat(result.exception)
                    .isNotNull()
                    .isInstanceOf(NotImplementedError::class)
                    .hasMessage("test message")
        }
    }
})