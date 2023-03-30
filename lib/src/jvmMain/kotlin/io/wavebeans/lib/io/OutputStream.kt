package io.wavebeans.lib.io

interface OutputStreamProvider {
    val stream: java.io.OutputStream
}

actual class ByteArrayOutputStream actual constructor() : OutputStream, OutputStreamProvider {

    override val stream = java.io.ByteArrayOutputStream()

    actual fun toByteArray(): ByteArray {
        return stream.toByteArray()
    }

    override fun write(b: Int) {
        stream.write(b)
    }

    override fun write(buffer: ByteArray) {
        stream.write(buffer)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
       stream.write(buffer, offset, length)
    }

    override fun flush() {
        stream.flush()
    }

    override fun close() {
        stream.close()
    }
}

actual class DataOutputStream actual constructor(stream: OutputStream) : OutputStream, OutputStreamProvider {

    override val stream = java.io.DataOutputStream(
        if (stream is OutputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}")
    )

    override fun write(byte: Int) {
        stream.write(byte)
    }

    override fun write(buffer: ByteArray) {
        stream.write(buffer)
    }

    actual fun writeInt(i: Int) {
        stream.writeInt(i)
    }

    actual fun writeShort(s: Int) {
        stream.writeShort(s)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        stream.write(buffer, offset, length)
    }

    override fun flush() {
        stream.flush()
    }

    override fun close() {
        stream.close()
    }
}

actual class BufferedOutputStream actual constructor(
    stream: OutputStream,
    bufferSize: Int
) : OutputStream, OutputStreamProvider {

    override val stream = java.io.BufferedOutputStream(
        if (stream is OutputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}"),
        bufferSize
    )

    override fun write(byte: Int) {
        stream.write(byte)
    }

    override fun write(buffer: ByteArray) {
        stream.write(buffer)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        stream.write(buffer, offset, length)
    }

    override fun flush() {
        stream.flush()
    }

    override fun close() {
        stream.close()
    }
}