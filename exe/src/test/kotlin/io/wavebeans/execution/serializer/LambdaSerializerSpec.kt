package io.wavebeans.execution.serializer

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.kotest.core.spec.style.DescribeSpec

class LambdaSerializerSpec : DescribeSpec({

    describe("Lambda serialization") {
        it("should serialize-deserialize lambda with 1 parameter") {
            val lambda = { p: Int -> p * 2 }

            val s = lambdaWrapper.serialize(lambda)
            val fn = lambdaWrapper.deserialize<Int, Int>(s)

            assertThat(fn(2)).isEqualTo(4)
        }
        it("should serialize-deserialize lambda with 2 parameters") {
            val lambda = { p: Int, q: Int -> p * q }

            val s = lambdaWrapper.serialize(lambda)
            val fn = lambdaWrapper.deserialize2<Int, Int, Int>(s)

            assertThat(fn(2, 3)).isEqualTo(6)
        }
        it("should serialize-deserialize lambda with 3 parameters") {
            val lambda = { p: Int, q: Int, r: Int -> p * q * r }

            val s = lambdaWrapper.serialize(lambda)
            val fn = lambdaWrapper.deserialize3<Int, Int, Int, Int>(s)

            assertThat(fn(2, 3, 4)).isEqualTo(24)
        }
        it("should serialize-deserialize lambda with 4 parameters") {
            val lambda = { p: Int, q: Int, r: Int, s: Int -> p * q * r * s }

            val s = lambdaWrapper.serialize(lambda)
            val fn = lambdaWrapper.deserialize4<Int, Int, Int, Int, Int>(s)

            assertThat(fn(2, 3, 4, 5)).isEqualTo(120)
        }
    }
})