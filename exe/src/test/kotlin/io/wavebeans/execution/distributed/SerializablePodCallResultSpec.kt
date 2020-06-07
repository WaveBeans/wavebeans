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
import kotlinx.serialization.protobuf.ProtoId
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object SerializablePodCallResultSpec : Spek({

    val builder = SerializablePodCallResultBuilder()

    fun result(value: Any?, e: Throwable?): SerializablePodCallResult {
        val call = Call.parseRequest("method?param=value")
        val r = e?.let { builder.error(call, it) } ?: builder.ok(call, value)

        return builder.fromInputStream(r.stream()) as SerializablePodCallResult
    }

    fun result(value: Any?): SerializablePodCallResult = result(value, null)

    fun result(value: Throwable): PodCallResult = result(null, value)


    describe("Wrapping value") {

        describe("serializable object") {

            @Serializable
            data class Clazz(
                    @ProtoId(1)
                    val s: String,
                    @ProtoId(2)
                    val l: Long,
                    @ProtoId(3)
                    val d: Double,
                    @ProtoId(4)
                    val lb: List<Boolean>
            )

            val clazz = Clazz("1", 2, 3.0, listOf(true, false))

            val result by memoized(SCOPE) { result(clazz) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<Clazz>()).isEqualTo(clazz) }

        }

        describe("Primitives") {
            describe("Long") {
                val result by memoized(SCOPE) { result(123L) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<Long>()).isEqualTo(123L) }
            }
            describe("Int") {
                val result by memoized(SCOPE) { result(123) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<Long>()).isEqualTo(123) }
            }
            describe("Double") {
                val result by memoized(SCOPE) { result(123.0) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<Long>()).isEqualTo(123.0) }
            }
            describe("Float") {
                val result by memoized(SCOPE) { result(123.0f) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<Long>()).isEqualTo(123.0f) }
            }
            describe("Boolean") {
                val result by memoized(SCOPE) { result(true) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<Long>()).isEqualTo(true) }
            }
        }

        describe("Sample") {
            val value = sampleOf(1.0)
            val result by memoized(SCOPE) { result(value) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<Sample>()).isEqualTo(value) }
        }

        describe("Unit") {
            val result by memoized(SCOPE) { result(Unit) }

            it("should have non empty obj") { assertThat(result.obj).isEqualTo(kotlin.Unit) }
            it("should have empty exception") { assertThat(result.exception).isNull() }
        }

        describe("null") {
            val result by memoized(SCOPE) { result(null) }

            it("should have non empty obj") { assertThat(result.obj).isNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
        }

        describe("Primitive arrays") {
            describe("double array") {
                val result by memoized(SCOPE) { result(DoubleArray(65536) { 1.0 }) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<DoubleArray>()).each { it.isEqualTo(1.0) } }
            }

            describe("byte array") {
                val result by memoized(SCOPE) { result(ByteArray(65536) { 1.toByte() }) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<ByteArray>()).each { it.isEqualTo(1.toByte()) } }
            }

            describe("int array") {
                val result by memoized(SCOPE) { result(IntArray(65536) { 1 }) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<IntArray>()).each { it.isEqualTo(1) } }
            }

            describe("float array") {
                val result by memoized(SCOPE) { result(FloatArray(65536) { 1.0f }) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<FloatArray>()).each { it.isEqualTo(1.0f) } }
            }

            describe("long array") {
                val result by memoized(SCOPE) { result(LongArray(65536) { 1L }) }

                it("should have non empty obj") { assertThat(result.obj).isNotNull() }
                it("should have empty exception") { assertThat(result.exception).isNull() }
                it("should return valid value") { assertThat(result.value<LongArray>()).each { it.isEqualTo(1L) } }
            }
        }

        describe("list of samples") {
            val sampleList = listOf(sampleOf(1), sampleOf(2))
            val result by memoized(SCOPE) { result(sampleList) }

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

        describe("empty list of samples") {
            val sampleList = listOf<Sample>()
            val result by memoized(SCOPE) { result(sampleList) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<List<Sample>>()).isEqualTo(sampleList) }
        }

        describe("list of lists of objects") {
            @Serializable
            data class Obj(
                    @ProtoId(1)
                    val a: Int
            )

            val list = listOf(
                    listOf(Obj(1), Obj(2), Obj(3)),
                    listOf(Obj(4), Obj(5)),
                    listOf(Obj(6))
            )

            val result by memoized(SCOPE) { result(list) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<List<List<Obj>>>()).isEqualTo(list) }
        }

        describe("medium") {

            val obj = SerializableMediumBuilder().from(listOf(sampleOf(1.0), sampleOf(1.1), sampleOf(-0.1)))

            val result by memoized(SCOPE) { result(obj) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<SerializableMedium>()).isEqualTo(obj) }
        }

        describe("windows") {

            val obj = Window<Int>(6, 2, listOf(1, 2, 3, 4, 5, 6)) { 0 }

            val result by memoized(SCOPE) { result(obj) }

            it("should have non empty obj") { assertThat(result.obj).isNotNull() }
            it("should have empty exception") { assertThat(result.exception).isNull() }
            it("should return valid value") { assertThat(result.value<Window<Int>>()).isEqualTo(obj) }
        }
    }

    describe("Wrapping errors") {

        describe("Wrapping Exception") {
            val result by memoized(SCOPE) { result(IllegalStateException("test message")) }

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
            val result by memoized(SCOPE) { result(IllegalStateException("test message", IllegalArgumentException("some cause"))) }

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
            val result by memoized(SCOPE) { result(NotImplementedError("test message")) }

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