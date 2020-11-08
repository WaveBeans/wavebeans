package io.wavebeans.lib

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.stream.window.Window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SampleVectorSpec : Spek({
    describe("Creation") {
        it("should be created of list of samples") {
            val a = sampleVectorOf(listOf(1, 2, 3, 4, 5).map { sampleOf(it) })
            assertThat(a).all {
                size().isEqualTo(5)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
                prop("3") { it[3] }.isEqualTo(sampleOf(4))
                prop("4") { it[4] }.isEqualTo(sampleOf(5))
            }
        }
        it("should be created of samples") {
            val a = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3), sampleOf(4), sampleOf(5))
            assertThat(a).all {
                size().isEqualTo(5)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
                prop("3") { it[3] }.isEqualTo(sampleOf(4))
                prop("4") { it[4] }.isEqualTo(sampleOf(5))
            }
        }
        it("should be created of window of sample") {
            val a = sampleVectorOf(Window(5, 5, listOf(1, 2, 3, 4, 5).map { sampleOf(it) }) { ZeroSample })
            assertThat(a).all {
                size().isEqualTo(5)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
                prop("3") { it[3] }.isEqualTo(sampleOf(4))
                prop("4") { it[4] }.isEqualTo(sampleOf(5))
            }
        }
    }

    describe("Plus operator") {
        it("should sum up the vectors of the same size") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4), sampleOf(5), sampleOf(6))

            assertThat(a1 + a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) + sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2) + sampleOf(5))
                prop("2") { it[2] }.isEqualTo(sampleOf(3) + sampleOf(6))
            }
        }
        it("should sum up the vectors of different sizes") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4))

            assertThat(a1 + a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) + sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should sum up the vectors if one of them is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a1 + a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should sum up the vectors if another is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a2 + a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should sum up the vectors if one of them is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a1 + a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should sum up the vectors if another is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a2 + a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should sum up the vectors if both of them are empty") {
            val a1 = sampleVectorOf()
            val a2 = sampleVectorOf()

            assertThat(a1 + a2).isNotNull().isEmpty()

        }
        it("should sum up the vectors if both of them are null") {
            val a1: SampleVector? = null
            val a2: SampleVector? = null

            assertThat(a1 + a2).isNull()
        }
    }

    describe("Times operator") {
        it("should multiply the vectors of the same size") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4), sampleOf(5), sampleOf(6))

            assertThat(a1 * a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) * sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2) * sampleOf(5))
                prop("2") { it[2] }.isEqualTo(sampleOf(3) * sampleOf(6))
            }
        }
        it("should multiply the vectors of different sizes") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4))

            assertThat(a1 * a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) * sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2) * ZeroSample)
                prop("2") { it[2] }.isEqualTo(sampleOf(3) * ZeroSample)
            }
        }
        it("should multiply the vectors if one of them is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a1 * a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) * ZeroSample)
                prop("1") { it[1] }.isEqualTo(sampleOf(2) * ZeroSample)
                prop("2") { it[2] }.isEqualTo(sampleOf(3) * ZeroSample)
            }
        }
        it("should multiply the vectors if another is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a2 * a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(ZeroSample * sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample * sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample * sampleOf(3))
            }
        }
        it("should multiply the vectors if one of them is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a1 * a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) * ZeroSample)
                prop("1") { it[1] }.isEqualTo(sampleOf(2) * ZeroSample)
                prop("2") { it[2] }.isEqualTo(sampleOf(3) * ZeroSample)
            }
        }
        it("should multiply the vectors if another is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a2 * a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(ZeroSample * sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample * sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample * sampleOf(3))
            }
        }
        it("should multiply the vectors if both of them are empty") {
            val a1 = sampleVectorOf()
            val a2 = sampleVectorOf()

            assertThat(a1 * a2).isNotNull().isEmpty()

        }
        it("should multiply the vectors if both of them are null") {
            val a1: SampleVector? = null
            val a2: SampleVector? = null

            assertThat(a1 * a2).isNull()
        }
    }

    describe("Minus operator") {
        it("should subtract the vectors of the same size") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4), sampleOf(5), sampleOf(6))

            assertThat(a1 - a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) - sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2) - sampleOf(5))
                prop("2") { it[2] }.isEqualTo(sampleOf(3) - sampleOf(6))
            }
        }
        it("should subtract the vectors of different sizes") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4))

            assertThat(a1 - a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) - sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should subtract the vectors of different sizes reversed order") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4))

            assertThat(a2 - a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(4) - sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample - sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample - sampleOf(3))
            }
        }
        it("should subtract the vectors if one of them is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a1 - a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should subtract the vectors if another is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a2 - a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(ZeroSample - sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample - sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample - sampleOf(3))
            }
        }
        it("should subtract the vectors if one of them is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a1 - a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1))
                prop("1") { it[1] }.isEqualTo(sampleOf(2))
                prop("2") { it[2] }.isEqualTo(sampleOf(3))
            }
        }
        it("should subtract the vectors if another is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a2 - a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(ZeroSample - sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample - sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample - sampleOf(3))
            }
        }
        it("should subtract the vectors if both of them are empty") {
            val a1 = sampleVectorOf()
            val a2 = sampleVectorOf()

            assertThat(a1 - a2).isNotNull().isEmpty()

        }
        it("should subtract the vectors if both of them are null") {
            val a1: SampleVector? = null
            val a2: SampleVector? = null

            assertThat(a1 - a2).isNull()
        }
    }

    describe("Div operator") {
        it("should divide the vectors of the same size") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4), sampleOf(5), sampleOf(6))

            assertThat(a1 / a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) / sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2) / sampleOf(5))
                prop("2") { it[2] }.isEqualTo(sampleOf(3) / sampleOf(6))
            }
        }
        it("should divide the vectors of different sizes") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4))

            assertThat(a1 / a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) / sampleOf(4))
                prop("1") { it[1] }.isEqualTo(sampleOf(2) / ZeroSample)
                prop("2") { it[2] }.isEqualTo(sampleOf(3) / ZeroSample)
            }
        }
        it("should divide the vectors of different sizes reversed order") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf(sampleOf(4))

            assertThat(a2 / a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(4) / sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample / sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample / sampleOf(3))
            }
        }
        it("should divide the vectors if one of them is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a1 / a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) / ZeroSample)
                prop("1") { it[1] }.isEqualTo(sampleOf(2) / ZeroSample)
                prop("2") { it[2] }.isEqualTo(sampleOf(3) / ZeroSample)
            }
        }
        it("should divide the vectors if another is empty") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2 = sampleVectorOf()

            assertThat(a2 / a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(ZeroSample / sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample / sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample / sampleOf(3))
            }
        }
        it("should divide the vectors if one of them is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a1 / a2).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(sampleOf(1) / ZeroSample)
                prop("1") { it[1] }.isEqualTo(sampleOf(2) / ZeroSample)
                prop("2") { it[2] }.isEqualTo(sampleOf(3) / ZeroSample)
            }
        }
        it("should divide the vectors if another is null") {
            val a1 = sampleVectorOf(sampleOf(1), sampleOf(2), sampleOf(3))
            val a2: SampleVector? = null

            assertThat(a2 / a1).isNotNull().all {
                size().isEqualTo(3)
                prop("0") { it[0] }.isEqualTo(ZeroSample / sampleOf(1))
                prop("1") { it[1] }.isEqualTo(ZeroSample / sampleOf(2))
                prop("2") { it[2] }.isEqualTo(ZeroSample / sampleOf(3))
            }
        }
        it("should divide the vectors if both of them are empty") {
            val a1 = sampleVectorOf()
            val a2 = sampleVectorOf()

            assertThat(a1 / a2).isNotNull().isEmpty()

        }
        it("should divide the vectors if both of them are null") {
            val a1: SampleVector? = null
            val a2: SampleVector? = null

            assertThat(a1 / a2).isNull()
        }
    }
})