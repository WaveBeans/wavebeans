package io.wavebeans.lib.io

actual class BufferedInputStream actual constructor(stream: InputStream) : InputStream() {
    override fun read(): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

actual class DataInputStream actual constructor(stream: InputStream) : InputStream() {
    override fun read(): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    actual fun readInt(): Int {
        TODO("Not yet implemented")
    }

    actual fun readShort(): Short {
        TODO("Not yet implemented")
    }
}

actual abstract class InputStream: Closeable {
    actual abstract fun read(): Int
    actual abstract fun read(buf: ByteArray): Int
    actual abstract fun read(buf: ByteArray, offset: Int, length: Int): Int
}

actual class ByteArrayInputStream actual constructor(buffer: ByteArray) : InputStream() {
    override fun read(): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

actual fun InputStream.bufferedReader(): Reader {
    TODO("Not yet implemented")
}