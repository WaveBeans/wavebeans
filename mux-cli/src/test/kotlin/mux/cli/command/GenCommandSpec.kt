package mux.cli.command

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isInstanceOf
import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.lib.io.SineGeneratedInput
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object GenCommandSpec : Spek({

    describe("GenCommand") {
        val gen = GenCommand(Session())

        describe("Sine generator: 0.1 fs=50.0 d=16 f=10 a=1.0") {
            val generator = SineGeneratedInput(50.0f, 10.0, 1.0, 0.1)
            val scope = gen.newScope("sine 0.1 fs=50.0 d=16 f=10 a=1.0")

            it("should be AudioStreamScope with 5 samples") {
                assertThat(scope).isInstanceOf(AudioStreamScope::class)
                val expected = generator.asSequence(50.0f).toList()
                val actual = (scope as AudioStreamScope).samples.asSequence(50.0f).toList()
                actual.forEachIndexed { idx, v ->
                    assertThat(v, "expectedList=$expected actualList=$actual @ $idx").isCloseTo(expected[idx], 0.000001)
                }
            }
        }
    }

})