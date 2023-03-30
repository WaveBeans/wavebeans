package io.wavebeans.lib.io

actual class ByteArrayOutputStream actual constructor() : OutputStream {

    actual fun toByteArray(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun write(byte: Int) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

actual class DataOutputStream actual constructor(stream: OutputStream): OutputStream {
    override fun write(byte: Int) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    actual fun writeInt(i: Int) {
        TODO("Not yet implemented")
    }

    actual fun writeShort(s: Int) {
        TODO("Not yet implemented")
    }
}

actual class BufferedOutputStream actual constructor(stream: OutputStream, bufferSize: Int) :
    OutputStream {
    override fun write(byte: Int) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}