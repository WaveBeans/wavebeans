package mux.lib

import mux.lib.io.StreamOutput

class MuxRuntime(val sampleRate: Float) {

    private val outputs = ArrayList<StreamOutput>()

    fun addOutput(output: StreamOutput): MuxRuntime {
        outputs += output
        return this
    }

    fun execute() {
        // just mock implementation
        outputs.forEach {
            it.write(sampleRate, null, null)
            it.close()
        }
    }
}