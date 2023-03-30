package io.wavebeans.lib.io

expect abstract class InputStream(): Closeable {
    abstract fun read(): Int
    abstract fun read(buf: ByteArray): Int
    abstract fun read(buf: ByteArray, offset: Int, length: Int): Int
}

expect fun InputStream.bufferedReader(): Reader

expect class ByteArrayInputStream(buffer: ByteArray): InputStream

expect class BufferedInputStream(stream: InputStream) : InputStream

expect class DataInputStream(stream: InputStream) : InputStream {
    fun readInt(): Int
    fun readShort(): Short
}

interface Reader: Closeable {
    fun readLine(): String
    fun lines(): Sequence<String>
}


