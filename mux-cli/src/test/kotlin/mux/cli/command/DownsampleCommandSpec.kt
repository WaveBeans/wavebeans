package mux.cli.command

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import mux.cli.scope.AudioFileScope
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianAudioInput
import mux.lib.io.SineGeneratedInput
import mux.lib.stream.AudioSampleStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DownsampleCommandSpec : Spek({

    describe("DownsampleCommand") {
        val sampleStream = AudioSampleStream(
                ByteArrayLittleEndianAudioInput(
                        BitDepth.BIT_8,
                        ByteArray(100) { (it and 0xFF).toByte() }
                ),
                50.0f
        )
        val gen = DownsampleCommand(sampleStream)

        describe("rate 2") {
            val scope = gen.newScope("2")

            it("should be AudioFileScope with 50 samples") {
                assertThat(scope).isInstanceOf(AudioFileScope::class)
                val audioFileScope = scope as AudioFileScope
                assertThat(audioFileScope.samples().samplesCount()).isEqualTo(50)
                assertThat(audioFileScope.samples().sampleRate).isEqualTo(25.0f)
            }
        }
    }

})