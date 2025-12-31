package io.wavebeans.lib.io

interface InputStream: AutoCloseable {
    abstract fun read(): Int
    abstract fun read(buf: ByteArray): Int
    abstract fun read(buf: ByteArray, offset: Int, length: Int): Int
}

expect fun InputStream.bufferedReader(): Reader

expect class ByteArrayInputStream(buffer: ByteArray): InputStream {
    override fun read(): Int
    override fun read(buf: ByteArray): Int
    override fun read(buf: ByteArray, offset: Int, length: Int): Int
    override fun close()
}

expect class BufferedInputStream(stream: InputStream) : InputStream {
    override fun read(): Int
    override fun read(buf: ByteArray): Int
    override fun read(buf: ByteArray, offset: Int, length: Int): Int
    override fun close()
}

expect class DataInputStream(stream: InputStream) : InputStream {
    fun readInt(): Int
    fun readShort(): Short
    override fun read(): Int
    override fun read(buf: ByteArray): Int
    override fun read(buf: ByteArray, offset: Int, length: Int): Int
    override fun close()
}

interface Reader: AutoCloseable {
    fun readLine(): String
    fun lines(): Sequence<String>
}


