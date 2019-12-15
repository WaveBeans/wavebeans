package io.wavebeans.cli.script

import io.wavebeans.execution.*
import io.wavebeans.lib.io.StreamOutput

class LocalDistributedScriptEvaluator(
        private val partitions: Int,
        private val threads: Int
) : ScriptEvaluator {

    override val outputs = ArrayList<StreamOutput<*>>()

    @UseExperimental(ExperimentalStdlibApi::class)
    override fun eval(sampleRate: Float) {

        Overseer().use {
            try {
                it.deployTopology(
                        outputs.buildTopology()
                                .partition(partitions)
                                .groupBeans(),
                        threads
                ).waitToFinish()
            } catch (e: InterruptedException) {
                // if it's interrupted then we need to gracefully
                // close everything that we've already processed
                it.close()
            }
        }
    }

}
