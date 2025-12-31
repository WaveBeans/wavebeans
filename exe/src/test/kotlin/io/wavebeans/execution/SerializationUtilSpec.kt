package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.lib.stream.AfterFillingFiniteStreamParams
import kotlinx.serialization.modules.SerializersModule

class SerializationUtilSpec : DescribeSpec({
    describe("Bean params") {
        val json by lazy {
            jsonPretty(SerializersModule {
                beanParams()
            })
        }
        describe("AfterFillingFiniteStreamParams") {
            it("should serialize") {
                val v = AfterFillingFiniteStreamParams(zeroFiller = 0)
                val s = json.encodeToString(v)

                assertThat(s)
                    .transform { json.decodeFromString<AfterFillingFiniteStreamParams<Int>>(s) }
                    .isEqualTo(v)
            }
        }
    }
})