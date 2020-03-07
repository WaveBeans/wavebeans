package io.wavebeans.cli.script

import mu.KotlinLogging
import java.io.File
import java.net.URLClassLoader

class ScriptClassLoader(jarFile: File) : URLClassLoader(
        arrayOf(jarFile.absoluteFile.toURI().toURL()),
        Thread.currentThread().contextClassLoader
) {

    private val log = KotlinLogging.logger { }

    override fun loadClass(name: String?): Class<*> {
        log.debug { "Attempting to load class $name" }
        return try {
            super.loadClass(name)
        } catch (e: Throwable) {
            log.error { "Can't load class $name" }
            throw e
        }
    }
}

