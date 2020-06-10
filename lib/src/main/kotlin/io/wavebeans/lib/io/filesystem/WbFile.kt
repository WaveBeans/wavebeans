package io.wavebeans.lib.io.filesystem

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * An abstraction thal allows to work with files whenever they are located. JDK free interface.
 */
interface WbFile {
    /**
     * Check if the file exists.
     */
    fun exists(): Boolean

    /**
     * Deletes the file on the location.
     */
    fun delete(): Boolean

    fun createWbFileOutputStream(): WbFileOutputStream

    fun createWbFileInputStream(): WbFileInputStream
}

interface WbFileFactory {

    companion object {

        private val registry = hashMapOf<String, WbFileFactory>()

        init {
            registry["file"] = LocalWbFileFactory
        }

        fun instance(scheme: String): WbFileFactory =
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

/**
 * WbFile output stream. Basically a copy of [java.io.OutputStream].
 */
abstract class WbFileOutputStream : OutputStream(), Closeable {
}

/**
 * WbFile input stream. Basically a copy of [java.io.InputStream].
 */
abstract class WbFileInputStream : InputStream(), Closeable {

}