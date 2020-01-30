package io.wavebeans.cli.script

import io.wavebeans.lib.io.StreamOutput
import java.io.Closeable
import java.util.concurrent.Future

interface ScriptEvaluator : Closeable {

    val outputs: MutableList<StreamOutput<*>>

    fun addOutput(out: StreamOutput<*>) {
        outputs += out
    }

    fun eval(sampleRate: Float): List<Future<Boolean>>
}

