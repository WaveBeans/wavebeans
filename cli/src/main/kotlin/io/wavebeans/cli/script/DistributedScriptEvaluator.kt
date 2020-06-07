package io.wavebeans.cli.script

import io.wavebeans.execution.*
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.lib.io.StreamOutput
import java.io.File
import java.util.concurrent.Future

class DistributedScriptEvaluator(
        private val partitions: Int,
        private val facilitatorLocations: List<String>,
        private val httpLocations: List<String> = emptyList()
) : ScriptEvaluator, SupportClassUploading {

    override val outputs = ArrayList<StreamOutput<*>>()

    private lateinit var overseer: DistributedOverseer
    private var additionalClasses: Map<String, File>? = null

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        overseer = DistributedOverseer(
                outputs,
                facilitatorLocations,
                httpLocations,
                partitions,
                additionalClasses = additionalClasses ?: emptyMap(),
                ignoreLocations = listOf(
                        ".*kotlin-main-kts-[\\d.]+.jar".toRegex(),
                        ".*kotlin-compiler-embeddable-[\\d.]+.jar".toRegex(),
                        ".*kotlin-scripting-compiler-embeddable-[\\d.]+.jar".toRegex(),
                        ".*kotlin-scripting-compiler-impl-embeddable-[\\d.]+.jar".toRegex(),
                        ".*kotlin-scripting-jvm-[\\d.]+.jar".toRegex(),
                        ".*kotlin-scripting-jvm-host-embeddable-[\\d.]+.jar".toRegex(),
                        ".*kotlin-scripting-common-[\\d.]+.jar".toRegex(),
                        ".*kotlin-script-runtime-[\\d.]+.jar".toRegex(),
                        ".*kotlin-daemon-embeddable-[\\d.]+.jar".toRegex(),
                        ".*trove4j-[\\d.]+.jar".toRegex(),
                        ".*wavebeans/lib/cli-[\\d.]+.jar".toRegex(),
                        ".*wavebeans/lib/http-[\\d.]+.jar".toRegex(),
                        ".*wavebeans/lib/exe-[\\d.]+.jar".toRegex()
                )
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
