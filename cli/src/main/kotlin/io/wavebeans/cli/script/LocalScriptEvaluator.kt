package io.wavebeans.cli.script

import io.wavebeans.execution.LocalOverseer
import io.wavebeans.execution.Overseer
import io.wavebeans.lib.io.StreamOutput
import java.util.concurrent.Future

class LocalScriptEvaluator : ScriptEvaluator {

    override val outputs = ArrayList<StreamOutput<*>>()

    private lateinit var overseer: Overseer

    override fun eval(sampleRate: Float): List<Future<Boolean>> {
        overseer = LocalOverseer(outputs)
        return overseer.eval(sampleRate)
    }

    override fun close() {
        overseer.close()
    }
}