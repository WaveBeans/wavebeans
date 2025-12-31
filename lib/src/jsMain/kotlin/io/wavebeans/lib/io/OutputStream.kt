package io.wavebeans.lib.io

actual class ByteArrayOutputStream actual constructor() : OutputStream {
    private var buf: ByteArray = ByteArray(32)
    private var count: Int = 0
    private var closed: Boolean = false

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity <= buf.size) return
        var newCap = buf.size.coerceAtLeast(1)
        while (newCap < minCapacity) newCap = newCap * 2
        val newBuf = ByteArray(newCap)
        // copy existing
        for (i in 0 until count) newBuf[i] = buf[i]
        buf = newBuf
    }

    actual fun toByteArray(): ByteArray {
        val out = ByteArray(count)
        for (i in 0 until count) out[i] = buf[i]
        return out
    }

    actual override fun write(byte: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        ensureCapacity(count + 1)
        buf[count++] = (byte and 0xFF).toByte()
    }

    actual override fun write(buffer: ByteArray) {
        write(buffer, 0, buffer.size)
    }

    actual override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        if (offset < 0 || length < 0 || offset + length > buffer.size) throw IndexOutOfBoundsException()
        if (length == 0) return
        ensureCapacity(count + length)
        for (i in 0 until length) {
            buf[count + i] = buffer[offset + i]
        }
        count += length
    }

    actual override fun flush() {
        // no-op for in-memory stream
    }

    actual override fun close() {
        closed = true
    }
}

actual class DataOutputStream actual constructor(stream: OutputStream): OutputStream {
    private val delegate: OutputStream = stream
    private var closed: Boolean = false

    actual override fun write(byte: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        delegate.write(byte)
    }

    actual override fun write(buffer: ByteArray) {
        if (closed) throw IllegalStateException("Stream is closed")
        delegate.write(buffer)
    }

    actual override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        delegate.write(buffer, offset, length)
    }

    actual override fun flush() {
        if (closed) return
        delegate.flush()
    }

    actual override fun close() {
        closed = true
        delegate.close()
    }

    actual fun writeInt(i: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        // big-endian
        delegate.write((i ushr 24) and 0xFF)
        delegate.write((i ushr 16) and 0xFF)
        delegate.write((i ushr 8) and 0xFF)
        delegate.write(i and 0xFF)
    }

    actual fun writeShort(s: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        // big-endian, only low 16 bits are written
        delegate.write((s ushr 8) and 0xFF)
        delegate.write(s and 0xFF)
    }
}

actual class BufferedOutputStream actual constructor(stream: OutputStream, bufferSize: Int) :
    OutputStream {
    private val delegate: OutputStream = stream
    private val buf: ByteArray = ByteArray(if (bufferSize > 0) bufferSize else 1)
    private var count: Int = 0
    private var closed: Boolean = false

    private fun flushBuffer() {
        if (count > 0) {
            delegate.write(buf, 0, count)
            count = 0
        }
    }

    actual override fun write(byte: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        if (count >= buf.size) flushBuffer()
        buf[count++] = (byte and 0xFF).toByte()
    }

    actual override fun write(buffer: ByteArray) {
        write(buffer, 0, buffer.size)
    }

    actual override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (closed) throw IllegalStateException("Stream is closed")
        if (offset < 0 || length < 0 || offset + length > buffer.size) throw IndexOutOfBoundsException()
        if (length == 0) return

        var off = offset
        var len = length
        // If the incoming data is larger than the buffer, flush current and write full chunks directly
        if (len >= buf.size) {
            flushBuffer()
            // write full buffer-size chunks directly to delegate to avoid copying
            var remaining = len
            var idx = off
            while (remaining >= buf.size) {
                delegate.write(buffer, idx, buf.size)
                idx += buf.size
                remaining -= buf.size
            }
            off = idx
            len = remaining
        }
        // Now len < buf.size; copy into internal buffer possibly across boundaries
        while (len > 0) {
            val space = buf.size - count
            if (space == 0) {
                flushBuffer()
                continue
            }
            val toCopy = if (len < space) len else space
            for (i in 0 until toCopy) {
                buf[count + i] = buffer[off + i]
            }
            count += toCopy
            off += toCopy
            len -= toCopy
        }
    }

    actual override fun flush() {
        if (closed) return
        flushBuffer()
        delegate.flush()
    }

    actual override fun close() {
        if (closed) return
        flush()
        closed = true
        delegate.close()
    }
}