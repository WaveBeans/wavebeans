package io.wavebeans.lib

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

internal class JavaClassLoader(val classLoader: java.lang.ClassLoader) : ClassLoader {
    override fun classForName(name: String): KClass<*> {
        return Class.forName(name, true, classLoader).kotlin
    }
}

actual object WaveBeansClassLoader {

    private val log = KotlinLogging.logger {}

    private val classLoaders = mutableListOf<ClassLoader>()

    init {
        reset()
    }

    actual fun reset() {
        classLoaders.clear()
        classLoaders += JavaClassLoader(WaveBeansClassLoader.javaClass.classLoader)
    }

    actual fun addClassLoader(classLoader: ClassLoader) {
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

    actual fun removeClassLoader(classLoader: ClassLoader): Boolean {
        return classLoaders.remove(classLoader)
    }

    actual fun classForName(name: String): KClass<*> {
        return tryPrimitives(name)
            ?: tryClassloaders(name)
            ?: throw ClassNotFoundException("$name class can't be loaded using any of class loaders: $classLoaders")
    }

    private fun tryClassloaders(name: String): KClass<*>? {
        val i = classLoaders.iterator()
        var clazz: KClass<*>? = null
        while (i.hasNext()) {
            val instance = i.next()
            try {
                clazz = instance.classForName(name)
                break
            } catch (e: ClassNotFoundException) {
                // ignore, try next one
            } catch (e: NoClassDefFoundError) {
                // ignore, try next one
            }
        }
        return clazz
    }

    private fun tryPrimitives(name: String): KClass<*>? {
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
            else -> null
        }
    }
}
