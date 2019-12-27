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

    describe("Init params") {

        describe("Primitive types") {
            data class Result(
                    val long: Long,
                    val int: Int,
                    val float: Float,
                    val double: Double,
                    val string: String
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, Result>(initParameters) {
                override fun apply(argument: Int): Result {
                    val long = initParams.long("long")
                    val int = initParams.int("int")
                    val float = initParams.float("float")
                    val double = initParams.double("double")
                    val string = initParams.string("string")
                    return Result(long, int, float, double, string)
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                                        .add("long", 1L)
                                        .add("int", 2)
                                        .add("float", 3.0f)
                                        .add("double", 4.0)
                                        .add("string", "abc")
                        ).apply(1)
                ).isEqualTo(Result(
                        1L,
                        2,
                        3.0f,
                        4.0,
                        "abc"
                ))

            }
        }

        describe("Nullable primitive types") {
            data class Result(
                    val long: Long?,
                    val int: Int?,
                    val float: Float?,
                    val double: Double?,
                    val string: String?
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, Result>(initParameters) {
                override fun apply(argument: Int): Result {
                    val long = initParams.longOrNull("long")
                    val int = initParams.intOrNull("int")
                    val float = initParams.floatOrNull("float")
                    val double = initParams.doubleOrNull("double")
                    val string = initParams.stringOrNull("string")
                    return Result(long, int, float, double, string)
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                        ).apply(1)
                ).isEqualTo(Result(
                        null,
                        null,
                        null,
                        null,
                        null
                ))

            }
        }

        describe("Collection of primitive types") {
            data class Result(
                    val longList: List<Long>,
                    val intList: List<Int>,
                    val floatList: List<Float>,
                    val doubleList: List<Double>,
                    val stringList: List<String>
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, Result>(initParameters) {
                override fun apply(argument: Int): Result {
                    val longs = initParams.longs("long")
                    val ints = initParams.ints("int")
                    val floats = initParams.floats("float")
                    val doubles = initParams.doubles("double")
                    val strings = initParams.strings("string")
                    return Result(longs, ints, floats, doubles, strings)
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                                        .addLongs("long", listOf(1L, 10L))
                                        .addInts("int", listOf(2, 20))
                                        .addFloats("float", listOf(3.0f, 30.0f))
                                        .addDoubles("double", listOf(4.0, 40.0))
                                        .addStrings("string", listOf("abc", "def"))
                        ).apply(1)
                ).isEqualTo(Result(
                        listOf(1L, 10L),
                        listOf(2, 20),
                        listOf(3.0f, 30.0f),
                        listOf(4.0, 40.0),
                        listOf("abc", "def")
                ))

            }
        }

        describe("Nullable collection of primitive types") {
            data class Result(
                    val longList: List<Long>?,
                    val intList: List<Int>?,
                    val floatList: List<Float>?,
                    val doubleList: List<Double>?,
                    val stringList: List<String>?
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, Result>(initParameters) {
                override fun apply(argument: Int): Result {
                    val longs = initParams.longsOrNull("long")
                    val ints = initParams.intsOrNull("int")
                    val floats = initParams.floatsOrNull("float")
                    val doubles = initParams.doublesOrNull("double")
                    val strings = initParams.stringsOrNull("string")
                    return Result(longs, ints, floats, doubles, strings)
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                        ).apply(1)
                ).isEqualTo(Result(
                        null,
                        null,
                        null,
                        null,
                        null
                ))

            }
        }

        describe("Custom types") {
            data class CustomType(
                    val long: Long,
                    val int: Int
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, CustomType>(initParameters) {
                override fun apply(argument: Int): CustomType {
                    return initParams.obj("obj") {
                        val (long, int) = it.split("|")
                        CustomType(long.toLong(), int.toInt())
                    }
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                                        .addObj("obj", CustomType(1L, 2)) { "${it.long}|${it.int}" }
                        ).apply(1)
                ).isEqualTo(CustomType(1L, 2))

            }
        }

        describe("Nullable custom types") {
            data class CustomType(
                    val long: Long,
                    val int: Int
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, CustomType?>(initParameters) {
                override fun apply(argument: Int): CustomType? {
                    return initParams.objOrNull("obj") {
                        throw UnsupportedOperationException("shouldn't be reachable")
                    }
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                        ).apply(1)
                ).isEqualTo(null)

            }
        }

        describe("Collection of custom types") {
            data class CustomType(
                    val long: Long,
                    val int: Int
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, List<CustomType>>(initParameters) {
                override fun apply(argument: Int): List<CustomType> {
                    return initParams.list("objs") {
                        val (long, int) = it.split("|")
                        CustomType(long.toLong(), int.toInt())
                    }
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                                        .add("objs", listOf(CustomType(1L, 2), CustomType(3L, 4))) { "${it.long}|${it.int}" }
                        ).apply(1)
                ).isEqualTo(listOf(CustomType(1L, 2), CustomType(3L, 4)))

            }
        }

        describe("Nullable collection of custom types") {
            data class CustomType(
                    val long: Long,
                    val int: Int
            )

            class Afn(initParameters: FnInitParameters) : Fn<Int, List<CustomType>?>(initParameters) {
                override fun apply(argument: Int): List<CustomType>? {
                    return initParams.listOrNull("objs") {
                        throw UnsupportedOperationException("shouldn't be reachable")
                    }
                }
            }

            it("should be indirectly instantiated and executed") {
                assertThat(
                        Fn.instantiate(
                                Afn::class.java,
                                FnInitParameters()
                        ).apply(1)
                ).isEqualTo(null)

            }
        }


    }
})