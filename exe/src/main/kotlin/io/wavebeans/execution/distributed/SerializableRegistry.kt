package io.wavebeans.execution.distributed

import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

object SerializableRegistry {

    private val registeredSerializers = mutableMapOf<KClass<*>, KSerializer<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> register(type: KClass<*>, serializer: KSerializer<T>) {
        registeredSerializers.putIfAbsent(type, serializer)
                ?.let { throw IllegalStateException("$type has already registered serializer $it") }
    }

    @Suppress("UNCHECKED_CAST")
    fun find(type: KClass<*>): KSerializer<Any> = registeredSerializers
            .filter { it.key == type || it.key.isSuperclassOf(type) }
            .map { it.value }
            .firstOrNull()
            ?.let { it as KSerializer<Any> }
            ?: serializer(type.java)

    init {
        register(List::class, ListObjectSerializer)
        register(Window::class, WindowOfAnySerializer)
        register(Pair::class, PairOfAnySerializer)
    }
}