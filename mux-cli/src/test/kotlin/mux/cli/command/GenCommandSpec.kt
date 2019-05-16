package mux.cli.command

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isInstanceOf
import mux.cli.scope.AudioFileScope
import mux.lib.io.SineGeneratedInput
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object GenCommandSpec : Spek({

    describe("GenCommand") {
        val gen = GenCommand()

        describe("Sine generator: 0.1 fs=50.0 d=16 f=10 a=1.0") {
            val generator = SineGeneratedInput(50.0f, 10.0, 1.0, 0.1)
            val scope = gen.newScope("sine 0.1 fs=50.0 d=16 f=10 a=1.0")

            it("should be AudioFileScope with 5 samples") {
                assertThat(scope).isInstanceOf(AudioFileScope::class)
                val expected = generator.asSequence().toList()
                val actual = (scope as AudioFileScope).samples().asSequence().toList()
                actual.forEachIndexed { idx, v ->
                    assertThat(v, "expectedList=$expected actualList=$actual @ $idx").isCloseTo(expected[idx], 0.000001)
                }
            }
        }
    }

})