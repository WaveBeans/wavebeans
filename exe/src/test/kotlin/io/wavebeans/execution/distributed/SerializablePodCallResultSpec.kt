package io.wavebeans.execution.distributed

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.execution.Call
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.value
import io.wavebeans.lib.Sample
import io.wavebeans.lib.sampleOf
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Serializable
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object SerializablePodCallResultSpec : Spek({

    val builder = SerializablePodCallResultBuilder()

    fun result(value: Any?, e: Throwable?): SerializablePodCallResult {
        val call = Call.parseRequest("method?param=value")
        val r = e?.let { builder.error(call, it) } ?: builder.ok(call, value)
        val buf = ByteArrayOutputStream().use {
            r.writeTo(it)
            it.flush()
            it.toByteArray()
        }

        return builder.fromInputStream(ByteArrayInputStream(buf)) as SerializablePodCallResult
    }

    fun result(value: Any?): SerializablePodCallResult = result(value, null)

    fun result(value: Throwable): PodCallResult = result(null, value)


    describe("Wrapping value") {

        describe("Wrapping Long") {

            val result by memoized { result(123L) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<Long>()).isEqualTo(123L) }

            describe("More Longs") {
                it("should wrap MAX_VALUE") { assertThat(result(Long.MAX_VALUE).value<Long>()).isEqualTo(Long.MAX_VALUE) }
                it("should wrap MIN_VALUE") { assertThat(result(Long.MIN_VALUE).value<Long>()).isEqualTo(Long.MIN_VALUE) }
                it("should wrap 0L") { assertThat(result(0L).value<Long>()).isEqualTo(0L) }
            }
        }

        describe("Wrapping Sample") {
            val value = sampleOf(1.0)
            val result by memoized { result(value) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<Sample>()).isEqualTo(sampleOf(1.0)) }
        }

        describe("Wrapping Unit") {
            val result by memoized { result(Unit) }

            it("should have non empty obj") { assertThat(result.obj).isEqualTo(kotlin.Unit) }
            it("should have empty exception") { assertThat(result.exception).isNull() }
        }

        describe("Wrapping null") {
            val result by memoized { result(null) }

            it("should have non empty obj") { assertThat(result.obj).isNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
        }

        describe("Wrapping list of samples") {
            val sampleList = listOf(sampleOf(1), sampleOf(2))
            val result by memoized { result(sampleList) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<List<Sample>>()).isEqualTo(sampleList) }

            describe("More sample lists") {
                it("should wrap 256+ lists") {
                    val longList = (0..256).map { sampleOf(it) }
                    assertThat(result(longList).value<List<Sample>>()).isEqualTo(longList)
                }
                it("should wrap 65536+ lists") {
                    val longList = (0..65536).map { sampleOf(it) }
                    assertThat(result(longList).value<List<Sample>>()).isEqualTo(longList)
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
                    assertThat(result(longList).value<List<Sample>>()).isEqualTo(longList)
                }
            }
        }

        describe("Wrapping empty list of samples") {
            val sampleList = listOf<Sample>()
            val result by memoized { result(sampleList) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<List<Sample>>()).isEqualTo(sampleList) }
        }

        describe("Wrapping list of lists of objects") {
            @Serializable
            data class Obj(val a: Int)

            val list = listOf(
                    listOf(Obj(1), Obj(2), Obj(3)),
                    listOf(Obj(4), Obj(5)),
                    listOf(Obj(6))
            )

            val result by memoized { result(list) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<List<List<Obj>>>()).isEqualTo(list) }
        }

        describe("Wrapping medium") {

            val obj = SerializableMediumBuilder().from(listOf(sampleOf(1.0), sampleOf(1.1), sampleOf(-0.1)))

            val result by memoized { result(obj) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<SerializableMedium>()).isEqualTo(obj) }
        }

        describe("Wrapping windows") {

            val obj = Window<Int>(6, 2, listOf(1, 2, 3, 4, 5, 6)) { 0 }

            val result by memoized { result(obj) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<Window<Int>>()).isEqualTo(obj) }
        }
    }

    describe("Wrapping errors") {

        describe("Wrapping Exception") {
            val result by memoized { result(IllegalStateException("test message")) }

            it("should be an exception") {
                assertThat(result.exception)
                        .isNotNull()
                        .isInstanceOf(CallingException::class).all {
                            prop("clazz") { it.inherentExceptionClazz }.isEqualTo(IllegalStateException::class)
                            prop("message") { it.inherentMessage }.isEqualTo("test message")
                            prop("stackTrace") { it.inherentStackTrace }.matchesPredicate { it.any { it.contains(SerializablePodCallResultSpec::class.simpleName!!) } }
                            prop("cause") { it.cause }.isNull()
                        }
            }
        }

        describe("Wrapping Exception with cause") {
            val result by memoized { result(IllegalStateException("test message", IllegalArgumentException("some cause"))) }

            it("should be an exception") {
                assertThat(result.exception)
                        .isNotNull()
                        .isInstanceOf(CallingException::class).all {
                            prop("clazz") { it.inherentExceptionClazz }.isEqualTo(IllegalStateException::class)
                            prop("message") { it.inherentMessage }.isEqualTo("test message")
                            prop("stackTrace") { it.inherentStackTrace }.matchesPredicate { it.any { it.contains(SerializablePodCallResultSpec::class.simpleName!!) } }
                            prop("cause") { it.cause }.isNotNull().all {
                                prop("clazz") { it.inherentExceptionClazz }.isEqualTo(IllegalArgumentException::class)
                                prop("message") { it.inherentMessage }.isEqualTo("some cause")
                                prop("stackTrace") { it.inherentStackTrace }.matchesPredicate { it.any { it.contains(SerializablePodCallResultSpec::class.simpleName!!) } }
                                prop("cause") { it.cause }.isNull()
                            }
                        }
            }
        }

        describe("Wrapping Error") {
            val result by memoized { result(NotImplementedError("test message")) }

            it("should be an error") {
                assertThat(result.exception)
                        .isNotNull()
                        .isInstanceOf(CallingException::class).all {
                            prop("clazz") { it.inherentExceptionClazz }.isEqualTo(NotImplementedError::class)
                            prop("message") { it.inherentMessage }.isEqualTo("test message")
                            prop("stackTrace") { it.inherentStackTrace }.matchesPredicate { it.any { it.contains(SerializablePodCallResultSpec::class.simpleName!!) } }
                            prop("cause") { it.cause }.isNull()
                        }
            }
        }
    }
})