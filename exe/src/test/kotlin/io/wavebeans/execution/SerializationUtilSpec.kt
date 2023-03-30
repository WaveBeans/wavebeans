package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.stream.AfterFillingFiniteStreamParams
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe

class SerializationUtilSpec : Spek({
    describe("Bean params") {
        val json by memoized(CachingMode.SCOPE) {
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