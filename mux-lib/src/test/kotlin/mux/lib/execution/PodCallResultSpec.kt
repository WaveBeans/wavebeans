package mux.lib.execution

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import mux.lib.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

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