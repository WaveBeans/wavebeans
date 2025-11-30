package io.wavebeans.lib.io

interface OutputStream: AutoCloseable {
    fun write(byte: Int)
    fun write(buffer: ByteArray)
    fun write(buffer: ByteArray, offset: Int, length: Int)
    fun flush()
}
expect class ByteArrayOutputStream(): OutputStream {
    fun toByteArray(): ByteArray
    override fun write(byte: Int)
    override fun write(buffer: ByteArray)
    override fun write(buffer: ByteArray, offset: Int, length: Int)
    override fun flush()
    override fun close()
}

expect class DataOutputStream(stream: OutputStream): OutputStream {
    fun writeInt(i: Int)
    fun writeShort(s: Int)
    override fun write(byte: Int)
    override fun write(buffer: ByteArray)
    override fun write(buffer: ByteArray, offset: Int, length: Int)
    override fun flush()
    override fun close()
}

expect class BufferedOutputStream(stream: OutputStream, bufferSize: Int): OutputStream {
    override fun write(byte: Int)
    override fun write(buffer: ByteArray)
    override fun write(buffer: ByteArray, offset: Int, length: Int)
    override fun flush()
    override fun close()
}
