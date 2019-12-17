package io.wavebeans.cli.script

import io.wavebeans.lib.io.StreamOutput

class LocalScriptEvaluator : ScriptEvaluator {

    override val outputs = ArrayList<StreamOutput<*>>()

    override fun eval(sampleRate: Float) {
        outputs
                .map { it.writer(sampleRate) }
                .forEach {
                    try {
                        while (it.write()) {
                            Thread.sleep(0)
                        }
                    } catch (e: InterruptedException) {
                        // if it's interrupted then we need to gracefully
                        // close everything that we've already processed
                    } finally {
                        it.close()
                    }
                }
    }

}