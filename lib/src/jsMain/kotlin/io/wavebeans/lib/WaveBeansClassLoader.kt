package io.wavebeans.lib

import kotlin.reflect.KClass

actual object WaveBeansClassLoader {
    actual fun reset() {
    }

    actual fun addClassLoader(classLoader: ClassLoader) {
    }

    actual fun removeClassLoader(classLoader: ClassLoader): Boolean {
        TODO("Not yet implemented")
    }

    actual fun classForName(name: String): KClass<*> {
        TODO("Not yet implemented")
    }
}