package io.wavebeans.lib

actual class URI actual constructor(uri: String) {
    actual val scheme: String
        get() = TODO("Not yet implemented")
    actual val path: String
        get() = TODO("Not yet implemented")

    actual fun asString(): String {
        TODO("Not yet implemented")
    }
}

actual class File actual constructor(path: String) {
    actual companion object {
        actual val separatorChar: Char
            get() = TODO("Not yet implemented")
    }

    actual val parent: String
        get() = TODO("Not yet implemented")
    actual val nameWithoutExtension: String
        get() = TODO("Not yet implemented")
    actual val extension: String
        get() = TODO("Not yet implemented")

}