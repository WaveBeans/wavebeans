package io.wavebeans.cli.script

import kotlin.reflect.KClass

enum class RunMode(val id: String, val clazz: KClass<out ScriptEvaluator>) {
    LOCAL("single-threaded", SingleThreadedScriptEvaluator::class),
    MULTI_THREADED("multi-threaded", MultiThreadedScriptEvaluator::class),
    DISTRIBUTED("distributed", DistributedScriptEvaluator::class)
    ;

    companion object {
        fun byId(id: String): RunMode? = values().firstOrNull { it.id == id }
    }
}