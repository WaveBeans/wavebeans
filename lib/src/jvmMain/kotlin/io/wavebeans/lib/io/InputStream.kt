package io.wavebeans.lib.io

interface InputStreamProvider {
    val stream: java.io.InputStream
}

actual class BufferedInputStream actual constructor(stream: InputStream) : InputStream, InputStreamProvider {

    override val stream = java.io.BufferedInputStream(
        if (stream is InputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}")
    )

    actual override fun read(): Int = stream.read()
    actual override fun read(buf: ByteArray): Int = stream.read(buf)
    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)
    actual override fun close() = stream.close()
}

actual class DataInputStream actual constructor(stream: InputStream) : InputStream, InputStreamProvider {

    override val stream = java.io.DataInputStream(
        if (stream is InputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}")
    )

    actual override fun read(): Int = stream.read()
    actual override fun read(buf: ByteArray): Int = stream.read(buf)
    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)
    actual override fun close() = stream.close()
    actual fun readInt(): Int = stream.readInt()
    actual fun readShort(): Short = stream.readShort()

}

actual class ByteArrayInputStream actual constructor(buffer: ByteArray) : InputStream, InputStreamProvider {

    override val stream = java.io.ByteArrayInputStream(buffer)

    actual override fun read(): Int = stream.read()
    actual override fun read(buf: ByteArray): Int = stream.read(buf)
    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)
    actual override fun close() = stream.close()

}

actual fun InputStream.bufferedReader(): Reader {
    return BufferedReader(this)
}

class BufferedReader(stream: InputStream) : Reader {

    private val reader = if (stream is InputStreamProvider) {
        java.io.BufferedReader(stream.stream.reader())
    } else {
        throw UnsupportedOperationException("Can't create buffered reader for $this")
    }

    override fun close() = reader.close()

    override fun readLine(): String = reader.readLine()

    override fun lines(): Sequence<String> = reader.lineSequence()
}