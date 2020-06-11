package io.wavebeans.fs.core

import java.net.URI

interface WbFileDriver {

    companion object {

        private val registry = hashMapOf<String, WbFileDriver>()

        fun registerDriver(scheme: String, driver: WbFileDriver) {
            registry.putIfAbsent(scheme, driver)
                    ?.let { throw IllegalStateException("Scheme $scheme is already registered: $it") }
        }

        fun unregisterDriver(scheme: String): WbFileDriver? = registry.remove(scheme)

        init {
            registerDriver("file", LocalWbFileDriver)
        }

        fun instance(scheme: String): WbFileDriver =
                registry[scheme.toLowerCase()]
                        ?: throw IllegalArgumentException("Scheme $scheme can be found among registered $registry")

        fun createFile(uri: URI): WbFile =
                registry[uri.scheme.toLowerCase()]
                        ?.createWbFile(uri)
                        ?: throw IllegalArgumentException("Can't lcoate correct factory for URI $uri among registered $registry")

    }

    fun createTemporaryWbFile(prefix: String, suffix: String, parent: WbFile? = null): WbFile

    fun createWbFile(uri: URI): WbFile

}