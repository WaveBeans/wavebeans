package io.wavebeans.cli.script

import io.wavebeans.lib.io.StreamOutput

interface ScriptEvaluator {

    val outputs: MutableList<StreamOutput<*>>

    fun addOutput(out: StreamOutput<*>) {
        outputs += out
    }

    fun eval(sampleRate: Float)
}

