package io.wavebeans.lib

import mu.KotlinLogging

object WaveBeansClassLoader {

    private val log = KotlinLogging.logger {}

    var classLoader: ClassLoader = WaveBeansClassLoader.javaClass.classLoader
        set(value) {
            log.debug {
                "Set new class loader $value from:\n" +
                        Thread.currentThread().stackTrace
                                .drop(1)
                                .joinToString("\n") { it.toString() }
            }
            field = value
        }

    fun instance(): ClassLoader = classLoader

    fun classForName(name: String): Class<*> = Class.forName(name, true, instance())
}
