package io.wavebeans.cli.script

import kotlin.reflect.KClass

enum class RunMode(val id: String, val clazz: KClass<out ScriptEvaluator>) {
    LOCAL("local", LocalScriptEvaluator::class),
    @ExperimentalStdlibApi
    LOCAL_DISTRIBUTED("local-distributed", LocalDistributedScriptEvaluator::class)
    ;

    companion object {
        fun byId(id: String): RunMode? = values().firstOrNull { it.id == id }
    }
}