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
        val i = classLoaders.iterator()
        while (i.hasNext()) {
            val instance = i.next()
            try {
                return Class.forName(name, true, instance)
            } catch (e: ClassNotFoundException) {
                // ignore, try next one
            } catch (e: NoClassDefFoundError) {
                // ignore, try next one
            }
        }
        throw ClassNotFoundException("$name class can't be loaded using any of class loaders: $classLoaders")
    }
}
