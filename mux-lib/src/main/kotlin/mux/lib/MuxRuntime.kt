package mux.lib

import mux.lib.io.StreamOutput
import java.lang.Thread.sleep
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

        val writers = outputs.map { it.writer(sampleRate) }

        var allRead = false
        while (!allRead) {
            allRead = writers.none { it.write(step, tu) }
            sleep(1)
        }

        writers.forEach { it.close() }
        outputs.forEach { it.close() }
    }
}