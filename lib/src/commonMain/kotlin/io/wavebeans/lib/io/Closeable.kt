package io.wavebeans.lib.io

interface Closeable {
    fun close()
}

fun <T: Closeable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this.close()
    }
}
