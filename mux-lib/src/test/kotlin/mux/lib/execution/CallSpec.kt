package mux.lib.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf


@ExperimentalStdlibApi
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> call(value: String): T =
        Call.parseRequest("method?param=$value").param("param", typeOf<T>()) as T

@ExperimentalStdlibApi
object CallSpec : Spek({

    describe("valid request parse") {

        it("should parse correctly method with one param") {
            assertThat(Call.parseRequest("method?param1=value1"))
                    .isEqualTo(Call("method", mapOf("param1" to "value1")))
        }

        it("should parse correctly method with two params") {
            assertThat(Call.parseRequest("method?param1=value1&param2=value2"))
                    .isEqualTo(Call("method", mapOf("param1" to "value1", "param2" to "value2")))
        }

        it("should parse correctly method with no params") {
            assertThat(Call.parseRequest("method"))
                    .isEqualTo(Call("method", emptyMap()))
        }
    }

    describe("request params conversion") {


        it("should convert int") {
            assertThat(call<Int>("1")).isEqualTo(1)
        }

        it("should convert long") {
            assertThat(call<Long>("1")).isEqualTo(1L)
        }

        describe("float type") {
            it("should convert int-like") { assertThat(call<Float>("1")).isEqualTo(1.0f) }
            it("should convert float-like 1.0f") { assertThat(call<Float>("1.0f")).isEqualTo(1.0f) }
            it("should convert double-like 1.0") { assertThat(call<Float>("1.0")).isEqualTo(1.0f) }
            it("should convert exponent notation 1e-0") { assertThat(call<Float>("1e-0")).isEqualTo(1.0f) }
            it("should convert exponent notation 1e0") { assertThat(call<Float>("1e0")).isEqualTo(1.0f) }
            it("should convert exponent notation 1e-12") { assertThat(call<Float>("1e-12")).isEqualTo(1e-12f) }
        }

        describe("boolean type") {
            it("should convert true") { assertThat(call<Boolean>("true")).isEqualTo(true) }
            it("should convert false") { assertThat(call<Boolean>("false")).isEqualTo(false) }
            it("should convert True") { assertThat(call<Boolean>("True")).isEqualTo(true) }
            it("should convert fAlse") { assertThat(call<Boolean>("fAlse")).isEqualTo(false) }
        }

        describe("TimeUnit type") {
            it("should convert MILLISECONDS") { assertThat(call<TimeUnit>("MILLISECONDS")).isEqualTo(MILLISECONDS) }
            it("should convert SECONDS") { assertThat(call<TimeUnit>("SECONDS")).isEqualTo(SECONDS) }
            it("should convert NANOSECONDS") { assertThat(call<TimeUnit>("NANOSECONDS")).isEqualTo(NANOSECONDS) }
            it("should convert MICROSECONDS") { assertThat(call<TimeUnit>("MICROSECONDS")).isEqualTo(MICROSECONDS) }
        }
    }
})