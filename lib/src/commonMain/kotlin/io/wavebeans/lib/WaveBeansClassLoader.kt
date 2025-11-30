package io.wavebeans.lib

import kotlin.reflect.KClass

interface ClassLoader {
    fun classForName(name: String): KClass<*>
}

expect object WaveBeansClassLoader {

    fun reset()

    fun addClassLoader(classLoader: ClassLoader)

    fun removeClassLoader(classLoader: ClassLoader): Boolean

    fun classForName(name: String): KClass<*>
}