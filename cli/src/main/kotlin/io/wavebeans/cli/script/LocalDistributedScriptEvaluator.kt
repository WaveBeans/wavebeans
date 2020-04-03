package io.wavebeans.cli.script

import io.wavebeans.execution.*
import io.wavebeans.lib.io.StreamOutput
import java.util.concurrent.Future

class LocalDistributedScriptEvaluator(
        private val partitions: Int,
        private val threads: Int
) : ScriptEvaluator {

    override val outputs = ArrayList<StreamOutput<*>>()

    private lateinit var overseer: Overseer

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        overseer = LocalDistributedOverseer(outputs, threads, partitions)
        return overseer.eval(sampleRate)
    }

    override fun close() {
        overseer.close()
    }
}
