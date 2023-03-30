package io.wavebeans.lib.io

import java.util.stream.Stream

interface InputStreamProvider {
    val stream: java.io.InputStream
}

actual class BufferedInputStream actual constructor(stream: InputStream) : InputStream(), InputStreamProvider {

    override val stream = java.io.BufferedInputStream(
        if (stream is InputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}")
    )

    override fun read(): Int = stream.read()
    override fun read(buf: ByteArray): Int = stream.read(buf)
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)
    override fun close() = stream.close()
}

actual class DataInputStream actual constructor(stream: InputStream) : InputStream(), InputStreamProvider {

    override val stream = java.io.DataInputStream(
        if (stream is InputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}")
    )

    override fun read(): Int = stream.read()
    override fun read(buf: ByteArray): Int = stream.read(buf)
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)
    override fun close() = stream.close()
    actual fun readInt(): Int = stream.readInt()
    actual fun readShort(): Short = stream.readShort()

}

actual abstract class InputStream actual constructor() : Closeable {
    actual abstract fun read(): Int
    actual abstract fun read(buf: ByteArray): Int
    actual abstract fun read(buf: ByteArray, offset: Int, length: Int): Int
}

actual class ByteArrayInputStream actual constructor(buffer: ByteArray) : InputStream(), InputStreamProvider {

    override val stream = java.io.ByteArrayInputStream(buffer)

    override fun read(): Int = stream.read()
    override fun read(buf: ByteArray): Int = stream.read(buf)
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)
    override fun close() = stream.close()

}

actual fun InputStream.bufferedReader(): Reader {
    return BufferedReader(this)
}

class BufferedReader(private val stream: InputStream) : Reader {

    private val reader = if (stream is InputStreamProvider) {
        java.io.BufferedReader(stream.stream.reader())

    } else {
        throw UnsupportedOperationException("Can't create buffered reader for $this")
    }

    override fun close() = reader.close()

    override fun readLine(): String = reader.readLine()

    override fun lines(): Sequence<String> = reader.lineSequence()
}