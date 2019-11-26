package io.wavebeans.cli.command

import assertk.assertThat
import assertk.assertions.isInstanceOf
import io.wavebeans.cli.Session
import io.wavebeans.cli.scope.AudioStreamScope
import io.wavebeans.lib.io.sine
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.xdescribe

object GenCommandSpec : Spek({

    xdescribe("GenCommand") {
        val gen = GenCommand(Session())

        describe("Sine generator: 0.1 fs=50.0 d=16 f=10 a=1.0") {
            val generator = 10.sine(1.0, 0.1)
            val scope = gen.newScope("sine 0.1 d=16 f=10 a=1.0")

            it("should be AudioStreamScope with 5 samples") {
                assertThat(scope).isInstanceOf(AudioStreamScope::class)
                val expected = generator.asSequence(50.0f).toList()
                val actual = (scope as AudioStreamScope).samples.asSequence(50.0f).toList()
                actual.forEachIndexed { idx, v ->
//                    assertThat(v, "expectedList=$expected actualList=$actual @ $idx").isCloseTo(expected[idx], 0.000001)
                    TODO()
                }
            }
        }
    }

})