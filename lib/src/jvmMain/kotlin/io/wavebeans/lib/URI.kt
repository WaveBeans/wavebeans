package io.wavebeans.lib

actual class URI actual constructor(uri: String) {

    private val uri = java.net.URI(uri)

    actual val scheme: String
        get() = uri.scheme

    actual val path: String
        get() = uri.path

    actual fun asString(): String = uri.toString()

    override fun toString(): String {
        return uri.toString()
    }
}

actual class File actual constructor(path: String) {
    actual companion object {
        actual val separatorChar: Char = java.io.File.separatorChar
    }

    private val file = java.io.File(path)

    actual val parent: String
        get() = file.parent
    actual val nameWithoutExtension: String
        get() = file.nameWithoutExtension
    actual val extension: String
        get() = file.extension

}