package io.wavebeans.lib.io

actual class BufferedInputStream actual constructor(stream: InputStream) : InputStream {
    private val delegate: InputStream = stream
    private var closed: Boolean = false
    private val bufferSize: Int = 8192
    private val buffer: ByteArray = ByteArray(bufferSize)
    private var pos: Int = 0
    private var limit: Int = 0

    private fun fillBuffer(): Int {
        // read into buffer from underlying stream
        pos = 0
        limit = 0
        val n = delegate.read(buffer, 0, buffer.size)
        if (n <= 0) return n // -1 on EOF or 0 if underlying allows
        limit = n
        return n
    }

    actual override fun read(): Int {
        if (closed) throw IllegalStateException("Stream is closed")
        if (pos >= limit) {
            val n = fillBuffer()
            if (n == -1) return -1
        }
        return buffer[pos++].toInt() and 0xFF
    }

    actual override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)

    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        if (closed) throw IllegalStateException("Stream is closed")
        if (offset < 0 || length < 0 || offset + length > buf.size) throw IndexOutOfBoundsException()
        if (length == 0) return 0

        var totalRead = 0
        while (totalRead < length) {
            if (pos >= limit) {
                val n = fillBuffer()
                if (n == -1) {
                    return if (totalRead == 0) -1 else totalRead
                }
            }
            val available = limit - pos
            val toCopy = if (length - totalRead < available) length - totalRead else available
            for (i in 0 until toCopy) {
                buf[offset + totalRead + i] = buffer[pos + i]
            }
            pos += toCopy
            totalRead += toCopy
            // Do not loop to read past one buffer fill if caller asked e.g. huge length; continue until filled or EOF
        }
        return totalRead
    }

    actual override fun close() {
        closed = true
        delegate.close()
    }
}

actual class DataInputStream actual constructor(stream: InputStream) : InputStream {
    private val delegate: InputStream = stream
    private var closed: Boolean = false

    actual override fun read(): Int {
        if (closed) throw IllegalStateException("Stream is closed")
        return delegate.read()
    }

    actual override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)

    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        if (closed) throw IllegalStateException("Stream is closed")
        return delegate.read(buf, offset, length)
    }

    actual override fun close() {
        closed = true
        delegate.close()
    }

    actual fun readInt(): Int {
        // big-endian: 4 bytes
        val b0 = read()
        if (b0 == -1) return -1
        val b1 = read(); if (b1 == -1) return -1
        val b2 = read(); if (b2 == -1) return -1
        val b3 = read(); if (b3 == -1) return -1
        return (b0 shl 24) or ((b1 and 0xFF) shl 16) or ((b2 and 0xFF) shl 8) or (b3 and 0xFF)
    }

    actual fun readShort(): Short {
        // big-endian: 2 bytes
        val b0 = read()
        if (b0 == -1) return (-1).toShort()
        val b1 = read(); if (b1 == -1) return (-1).toShort()
        val v = ((b0 and 0xFF) shl 8) or (b1 and 0xFF)
        return v.toShort()
    }
}

actual class ByteArrayInputStream actual constructor(buffer: ByteArray) : InputStream {
    private val buf: ByteArray = buffer
    private var pos: Int = 0
    private var closed: Boolean = false

    actual override fun read(): Int {
        if (closed) throw IllegalStateException("Stream is closed")
        if (pos >= buf.size) return -1
        return buf[pos++].toInt() and 0xFF
    }

    actual override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)

    actual override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        if (closed) throw IllegalStateException("Stream is closed")
        if (offset < 0 || length < 0 || offset + length > buf.size) throw IndexOutOfBoundsException()
        if (length == 0) return 0
        if (pos >= this.buf.size) return -1

        val available = this.buf.size - pos
        val toRead = if (length < available) length else available
        for (i in 0 until toRead) {
            buf[offset + i] = this.buf[pos + i]
        }
        pos += toRead
        return toRead
    }

    actual override fun close() {
        closed = true
    }
}

actual fun InputStream.bufferedReader(): Reader {
    val input = this
    class Utf8BufferedReader : Reader {
        private val stream: InputStream = BufferedInputStream(input)
        private var closed: Boolean = false
        private val lineBuffer = mutableListOf<Byte>()

        private fun decode(bytes: ByteArray): String {
            // Prefer global TextDecoder if available (Node >= 11 or modern browsers)
            val td = js("typeof TextDecoder !== 'undefined' ? new TextDecoder('utf-8') : null")
            return if (td != null) {
                td.decode(bytes.unsafeCast<dynamic>()) as String
            } else {
                // Fallback: naive decoding assuming latin-1 (may not handle multi-byte UTF-8 correctly)
                buildString(bytes.size) {
                    for (b in bytes) append(b.toInt().and(0xFF).toChar())
                }
            }
        }

        override fun readLine(): String {
            if (closed) throw IllegalStateException("Reader is closed")
            lineBuffer.clear()
            var sawAny = false
            while (true) {
                val r = stream.read()
                if (r == -1) {
                    if (!sawAny) throw NoSuchElementException("EOF")
                    // decode what we have (without newline)
                    val arr = lineBuffer.toByteArray()
                    return decode(arr)
                }
                sawAny = true
                val b = (r and 0xFF).toByte()
                if (b == '\n'.code.toByte()) {
                    // handle optional preceding CR
                    if (lineBuffer.isNotEmpty() && lineBuffer.last() == '\r'.code.toByte()) {
                        lineBuffer.removeAt(lineBuffer.lastIndex)
                    }
                    val arr = lineBuffer.toByteArray()
                    return decode(arr)
                } else {
                    lineBuffer.add(b)
                }
            }
        }

        override fun lines(): Sequence<String> = sequence {
            while (true) {
                val line = try {
                    readLine()
                } catch (e: NoSuchElementException) {
                    break
                }
                yield(line)
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            stream.close()
        }
    }
    return Utf8BufferedReader()
}