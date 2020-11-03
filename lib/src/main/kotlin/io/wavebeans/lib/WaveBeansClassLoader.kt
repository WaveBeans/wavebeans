package io.wavebeans.lib

import mu.KotlinLogging

object WaveBeansClassLoader {

    private val log = KotlinLogging.logger {}

    private val classLoaders = mutableListOf<ClassLoader>()

    init {
        reset()
    }

    fun reset() {
        classLoaders.clear()
        classLoaders += WaveBeansClassLoader.javaClass.classLoader
    }

    fun addClassLoader(classLoader: ClassLoader) {
        if (!classLoaders.contains(classLoader)) {
            log.debug {
                "Setting new class loader $classLoader from:\n" +
                        Thread.currentThread().stackTrace
                                .drop(1)
                                .joinToString("\n") { "\t at $it" }
            }
            classLoaders += classLoader
        }
    }

    fun removeClassLoader(classLoader: ClassLoader): Boolean {
        return classLoaders.remove(classLoader)
    }

    fun classForName(name: String): Class<*> {
        return tryPrimitives(name)
                ?: tryClassloaders(name)
                ?: throw ClassNotFoundException("$name class can't be loaded using any of class loaders: $classLoaders")
    }

    private fun tryClassloaders(name: String): Class<*>? {
        val i = classLoaders.iterator()
        var clazz: Class<*>? = null
        while (i.hasNext()) {
            val instance = i.next()
            try {
                clazz = Class.forName(name, true, instance)
                break
            } catch (e: ClassNotFoundException) {
                // ignore, try next one
            } catch (e: NoClassDefFoundError) {
                // ignore, try next one
            }
        }
        return clazz
    }

    private fun tryPrimitives(name: String): Class<*>? {
        return when (name) {
            "byte", "kotlin.Byte" -> Byte::class.java
            "short", "kotlin.Short" -> Short::class.java
            "int", "kotlin.Int" -> Int::class.java
            "long", "kotlin.Long" -> Long::class.java
            "float", "kotlin.Float" -> Float::class.java
            "double", "kotlin.Double" -> Double::class.java
            "ByteArray", "kotlin.ByteArray" -> ByteArray::class.java
            "ShortArray", "kotlin.ShortArray" -> ShortArray::class.java
            "IntArray", "kotlin.IntArray" -> IntArray::class.java
            "LongArray", "kotlin.LongArray" -> LongArray::class.java
            "FloatArray", "kotlin.FloatArray" -> FloatArray::class.java
            "DoubleArray", "kotlin.DoubleArray" -> DoubleArray::class.java
            "Any", "kotlin.Any" -> Any::class.java
            else -> null
        }
    }
}
