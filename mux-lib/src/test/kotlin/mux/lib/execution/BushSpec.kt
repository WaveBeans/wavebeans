package mux.lib.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.Sample
import mux.lib.stream.SampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.random.Random

val random = Random(1234)

@ExperimentalStdlibApi
object BushSpec : Spek({

    describe("Bush should call pod method. 1 pod per bush") {

        val podKey = PodKey(random.nextInt(), 0)
        val pod = object : Pod<Sample, SampleStream> {
            override val podKey: PodKey
                get() = podKey

            override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException()

            override val parameters: BeanParams
                get() = throw UnsupportedOperationException()

            fun methodWithNoParamsAndNoReturn(): Unit = Unit

            fun throwsIllegalStateException(): Unit = throw IllegalStateException("test message")

            fun throwsUnsupportedOperationException(): Unit = throw UnsupportedOperationException("test message")

            fun throwsNotImplementedError(): Unit = throw NotImplementedError("test message")

            fun convertsIntToLong(value: Int): Long = value.toLong()

        }
        val bush = Bush()
                .also { it.addPod(pod) }
                .also { it.start() }

        after {
            bush.close()
        }

        it("should not have params and return nothing") {
            val method = "methodWithNoParamsAndNoReturn"
            assertThat(bush.call(podKey, method)).all {
                prop("call") { it.call }.all {
                    prop("method") { it.method }.isEqualTo(method)
                    prop("params") { it.params }.isEmpty()
                }
                prop("byteArray") { it.byteArray }.isNull()
                prop("exception") { it.exception }.isNull()
            }

        }

        it("should return IllegalStateException") {
            val method = "throwsIllegalStateException"
            assertThat(bush.call(podKey, method)).all {
                prop("call") { it.call }.all {
                    prop("method") { it.method }.isEqualTo(method)
                    prop("params") { it.params }.isEmpty()
                }
                prop("byteArray") { it.byteArray }.isNull()
                prop("exception") { it.exception }
                        .isNotNull()
                        .isInstanceOf(IllegalStateException::class)
                        .hasMessage("test message")
            }

        }

        it("should return UnsupportedOperationException") {
            val method = "throwsUnsupportedOperationException"
            assertThat(bush.call(podKey, method)).all {
                prop("call") { it.call }.all {
                    prop("method") { it.method }.isEqualTo(method)
                    prop("params") { it.params }.isEmpty()
                }
                prop("byteArray") { it.byteArray }.isNull()
                prop("exception") { it.exception }
                        .isNotNull()
                        .isInstanceOf(UnsupportedOperationException::class)
                        .hasMessage("test message")
            }

        }

        it("should return NotImplementedError") {
            val method = "throwsNotImplementedError"
            assertThat(bush.call(podKey, method)).all {
                prop("call") { it.call }.all {
                    prop("method") { it.method }.isEqualTo(method)
                    prop("params") { it.params }.isEmpty()
                }
                prop("byteArray") { it.byteArray }.isNull()
                prop("exception") { it.exception }
                        .isNotNull()
                        .isInstanceOf(NotImplementedError::class)
                        .hasMessage("test message")
            }

        }

        it("should have int convertsIntToLong and return long") {
            val method = "convertsIntToLong"
            assertThat(bush.call(podKey, "$method?value=123")).all {
                prop("call") { it.call }.all {
                    prop("method") { it.method }.isEqualTo(method)
                    prop("params") { it.params }.isEqualTo(mapOf("value" to "123"))
                }
                prop("byteArray") { it.byteArray }.isNotNull()
                prop("exception") { it.exception }.isNull()
                prop("asLong") { it.long() }.isEqualTo(123L)
            }

        }


    }

})