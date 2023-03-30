package io.wavebeans.lib.io

interface OutputStream: Closeable {
    fun write(byte: Int)
    fun write(buffer: ByteArray)
    fun write(buffer: ByteArray, offset: Int, length: Int)
    fun flush()
}
expect class ByteArrayOutputStream(): OutputStream {
    fun toByteArray(): ByteArray
}

expect class DataOutputStream(stream: OutputStream): OutputStream {
    fun writeInt(i: Int)
    fun writeShort(s: Int)
}

expect class BufferedOutputStream(stream: OutputStream, bufferSize: Int): OutputStream
