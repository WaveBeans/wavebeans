package io.wavebeans.execution.distributed

import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass

class FacilitatorClassLoader(parent: ClassLoader) : io.wavebeans.lib.ClassLoader, URLClassLoader(emptyArray(), parent) {

    operator fun plusAssign(url: URL) {
        addURL(url)
    }

    override fun classForName(name: String): KClass<*> {
        return Class.forName(name, true, this).kotlin
    }
}