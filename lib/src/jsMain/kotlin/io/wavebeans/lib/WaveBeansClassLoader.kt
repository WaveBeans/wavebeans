package io.wavebeans.lib

import kotlin.reflect.KClass

actual object WaveBeansClassLoader {
    private val classLoaders = mutableListOf<ClassLoader>()

    actual fun reset() {
        classLoaders.clear()
    }

    actual fun addClassLoader(classLoader: ClassLoader) {
        if (!classLoaders.contains(classLoader)) {
            classLoaders += classLoader
        }
    }

    actual fun removeClassLoader(classLoader: ClassLoader): Boolean {
        return classLoaders.remove(classLoader)
    }

    actual fun classForName(name: String): KClass<*> {
        return when (name) {
            "byte", "kotlin.Byte" -> Byte::class
            "short", "kotlin.Short" -> Short::class
            "int", "kotlin.Int" -> Int::class
            "long", "kotlin.Long" -> Long::class
            "float", "kotlin.Float" -> Float::class
            "double", "kotlin.Double" -> Double::class
            "ByteArray", "kotlin.ByteArray" -> ByteArray::class
            "ShortArray", "kotlin.ShortArray" -> ShortArray::class
            "IntArray", "kotlin.IntArray" -> IntArray::class
            "LongArray", "kotlin.LongArray" -> LongArray::class
            "FloatArray", "kotlin.FloatArray" -> FloatArray::class
            "DoubleArray", "kotlin.DoubleArray" -> DoubleArray::class
            "Any", "kotlin.Any" -> Any::class
            else -> {
                var clazz: KClass<*>? = null
                for (loader in classLoaders) {
                    try {
                        clazz = loader.classForName(name)
                        break
                    } catch (e: Throwable) {
                        // ignore
                    }
                }
                clazz ?: throw Exception("Class $name not found")
            }
        }
    }
}