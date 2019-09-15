package mux.lib

import kotlin.reflect.KClass

data class MuxParams<T : Any, S : Any>(
        val streamClazz: KClass<S>,
        val elementClazz: KClass<T>,
        val parameters: Map<String, Any>
)