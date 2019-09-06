package mux.lib

import mux.lib.io.StreamOutput
import mux.lib.io.toMono8bitWav
import mux.lib.stream.sine
import mux.lib.stream.trim
import java.util.concurrent.TimeUnit

class MuxRuntime(val sampleRate: Float) {

    private val outputs = ArrayList<StreamOutput>()

    fun addOutput(output: StreamOutput): MuxRuntime {
        outputs += output
        return this
    }

    fun execute() {
        // just mock implementation
        val step = 10L
        val tu = TimeUnit.MILLISECONDS
        var current = 0L

        while (
                !outputs
                        .map { it.write(sampleRate, current, current + step, tu) }
                        .all { it }
        ) {
            current += step
        }


        outputs.forEach { it.close() }
    }
}

fun main() {
    val sine = 440.sine(0.5)
    MuxRuntime(44100.0f)
            .addOutput(sine.trim(1000).toMono8bitWav("file:///users/asubb/tmp/sine8.wav"))
            .execute()

}