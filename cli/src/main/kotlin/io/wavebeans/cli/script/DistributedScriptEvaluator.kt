package io.wavebeans.cli.script

import io.wavebeans.execution.*
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.lib.io.StreamOutput
import java.io.File
import java.util.concurrent.Future

class DistributedScriptEvaluator(
        private val partitions: Int,
        private val crewGardenerLocations: List<String>
) : ScriptEvaluator, SupportClassUploading {

    override val outputs = ArrayList<StreamOutput<*>>()

    private lateinit var overseer: DistributedOverseer
    private var additionalClasses: Map<String, File>? = null

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        overseer = DistributedOverseer(
                outputs,
                crewGardenerLocations,
                partitions,
                additionalClasses = additionalClasses ?: emptyMap()
        )
        return overseer.eval(sampleRate)
    }

    override fun close() {
        overseer.close()
    }

    override fun setAdditionalClassesDir(dir: File) {
        val classes = mutableMapOf<String, File>()
        fun iterateOver(f: File) {
            if (f.isDirectory) {
                f.listFiles().forEach { iterateOver(it) }
            } else if (f.extension == "class") {
                val classPath = dir.absoluteFile.toPath().relativize(f.absoluteFile.toPath()).toString()
                classes[classPath] = f
            }
        }
        iterateOver(dir)
        additionalClasses = classes
    }
}
