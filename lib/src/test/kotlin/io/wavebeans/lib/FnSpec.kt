package io.wavebeans.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.catch
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object FnSpec : Spek({

    describe("Define Fn without parameters using Lambda function") {

        describe("Without outer closure dependencies") {
            val fn = Fn.wrap<Int, Long> { it.toLong() }

            it("should return result") { assertThat(fn.apply(1)).isEqualTo(1L) }
        }

        describe("With outer closure dependencies") {
            val dependentValue = 1L

            it("should throw an exception during wrapping") {
                assertThat(catch { Fn.wrap<Int, Long> { it.toLong() * dependentValue } })
                        .isNotNull().isInstanceOf(IllegalArgumentException::class)
            }
        }

        describe("Lambda function wrapped and defined as Class") {
            val lambda: (Int) -> Long = { it.toLong() }
            val fn = Fn.wrap(lambda)

            val fnInstantiated = Fn.instantiate(fn::class.java, fn.initParams)

            it("should return result") { assertThat(fnInstantiated.apply(1)).isEqualTo(1L) }
        }
    }

    describe("Define Fn") {

        describe("No outer closure dependencies") {
            class AFn(initParameters: FnInitParameters) : Fn<Int, Long>(initParameters) {

                constructor(a: Int, b: Long, c: String) : this(FnInitParameters()
                        .add("a", a)
                        .add("b", b)
                        .add("c", c)
                )

                override fun apply(argument: Int): Long {
                    return if (initParams.string("c") == "withInt") {
                        argument * initParams.int("a").toLong()
                    } else {
                        argument * initParams.long("b")
                    }
                }

            }

            it("should return result") {
                assertThat(AFn(1, 1L, "withInt").apply(1)).isEqualTo(1L)
                assertThat(AFn(2, 1L, "withInt").apply(1)).isEqualTo(2L)
                assertThat(AFn(1, 1L, "notWithInt").apply(1)).isEqualTo(1L)
                assertThat(AFn(1, 2L, "notWithInt").apply(1)).isEqualTo(2L)
            }

            it("should be indirectly instantiated and executed") {
                assertThat(Fn.instantiate(
                        AFn::class.java,
                        FnInitParameters().add("a", 1).add("b", 1L).add("c", "withInt")
                ).apply(1))
                        .isEqualTo(1L)
            }
        }

        describe("Outer closure dependency") {
            val dependentValue = 1L

            class AFn(initParameters: FnInitParameters) : Fn<Int, Long>(initParameters) {
                override fun apply(argument: Int): Long = dependentValue
            }

            it("should throw an exception during indirect instantiation") {
                assertThat(catch { Fn.instantiate(AFn::class.java) })
                        .isNotNull()
                        .isInstanceOf(IllegalStateException::class)
            }
        }

        describe("No params required") {
            class AFn : Fn<Int, Long>() {
                override fun apply(argument: Int): Long = argument.toLong()
            }

            it("should be indirectly instantiated and executed") {
                assertThat(Fn.instantiate(AFn::class.java).apply(1)).isEqualTo(1L)

            }
        }
    }
})