package io.wavebeans.cli.script

import io.wavebeans.execution.ExecutionResult
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.execution.Overseer
import io.wavebeans.lib.io.StreamOutput
import java.util.concurrent.Future

class SingleThreadedScriptEvaluator : ScriptEvaluator {

    override val outputs = ArrayList<StreamOutput<*>>()

    private lateinit var overseer: Overseer

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        overseer = SingleThreadedOverseer(outputs)
        return overseer.eval(sampleRate)
    }

    override fun close() {
        overseer.close()
    }
}