package mux.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import mux.cli.Session
import mux.cli.scope.AudioStreamScope
import mux.lib.BitDepth
import mux.lib.io.ByteArrayLittleEndianAudioInput
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
        val gen = DownsampleCommand(Session(), sampleStream)

        describe("rate 2") {
            val scope = gen.newScope("2")

            it("should be AudioStreamScope with 50 samples") {
                assertThat(scope).isInstanceOf(AudioStreamScope::class)
                val audioFileScope = scope as AudioStreamScope
                assertThat(audioFileScope.samples().samplesCount()).isEqualTo(50)
                assertThat(audioFileScope.samples().sampleRate).isEqualTo(25.0f)
            }
        }
    }

})