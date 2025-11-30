package io.wavebeans.lib.io

interface OutputStreamProvider {
    val stream: java.io.OutputStream
}

actual class ByteArrayOutputStream actual constructor() : OutputStream, OutputStreamProvider {

    override val stream = java.io.ByteArrayOutputStream()

    actual fun toByteArray(): ByteArray {
        return stream.toByteArray()
    }

    actual override fun write(byte: Int) {
        stream.write(byte)
    }

    actual override fun write(buffer: ByteArray) {
        stream.write(buffer)
    }

    actual override fun write(buffer: ByteArray, offset: Int, length: Int) {
       stream.write(buffer, offset, length)
    }

    actual override fun flush() {
        stream.flush()
    }

    actual override fun close() {
        stream.close()
    }
}

actual class DataOutputStream actual constructor(stream: OutputStream) : OutputStream, OutputStreamProvider {

    override val stream = java.io.DataOutputStream(
        if (stream is OutputStreamProvider) stream.stream
        else throw UnsupportedOperationException("${stream::class}")
    )

    actual override fun write(byte: Int) {
        stream.write(byte)
    }

    actual override fun write(buffer: ByteArray) {
        stream.write(buffer)
    }

    actual fun writeInt(i: Int) {
        stream.writeInt(i)
    }

    actual fun writeShort(s: Int) {
        stream.writeShort(s)
    }

    actual override fun write(buffer: ByteArray, offset: Int, length: Int) {
        stream.write(buffer, offset, length)
    }

    actual override fun flush() {
        stream.flush()
    }

    actual override fun close() {
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

    actual override fun write(byte: Int) {
        stream.write(byte)
    }

    actual override fun write(buffer: ByteArray) {
        stream.write(buffer)
    }

    actual override fun write(buffer: ByteArray, offset: Int, length: Int) {
        stream.write(buffer, offset, length)
    }

    actual override fun flush() {
        stream.flush()
    }

    actual override fun close() {
        stream.close()
    }
}