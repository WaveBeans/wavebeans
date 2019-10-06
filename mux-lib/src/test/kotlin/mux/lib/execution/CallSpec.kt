package mux.lib.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.typeOf

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
            assertThat(Call.parseRequest("method?intParam=1").param("intParam", typeOf<Int>()))
                    .isEqualTo(1)
        }

        it("should convert long") {
            assertThat(Call.parseRequest("method?param=1").param("param", typeOf<Long>()))
                    .isEqualTo(1L)
        }

        it("should convert float") {
            assertThat(Call.parseRequest("method?param=1").param("param", typeOf<Float>()))
                    .isEqualTo(1.0f)

        }
    }
})