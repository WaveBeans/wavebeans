package io.wavebeans.lib.io

actual class BufferedInputStream actual constructor(stream: InputStream) : InputStream {
    actual override fun read(): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }
}

actual class DataInputStream actual constructor(stream: InputStream) : InputStream {
    actual override fun read(): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }

    actual fun readInt(): Int {
        TODO("Not yet implemented")
    }

    actual fun readShort(): Short {
        TODO("Not yet implemented")
    }
}

actual class ByteArrayInputStream actual constructor(buffer: ByteArray) : InputStream {
    actual override fun read(): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }
}

actual fun InputStream.bufferedReader(): Reader {
    TODO("Not yet implemented")
}