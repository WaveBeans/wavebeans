package io.wavebeans.lib

expect class URI(uri: String) {
    val scheme: String
    val path: String
    fun asString(): String
}

expect class File(path: String) {
    companion object {
        val separatorChar: Char
    }

    val parent: String
    val nameWithoutExtension: String
    val extension: String

}
