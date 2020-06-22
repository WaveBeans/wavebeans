package io.wavebeans.fs.local

import io.wavebeans.fs.core.WbFileInputStream
import java.io.FileInputStream

class LocalWbFileInputStream(wbFile: LocalWbFile) : WbFileInputStream() {

    private val stream = FileInputStream(wbFile.file)

    override fun skip(n: Long): Long = stream.skip(n)

    override fun available(): Int = stream.available()

    override fun reset() = stream.reset()

    override fun close() = stream.close()

    override fun mark(readlimit: Int) = stream.mark(readlimit)

    override fun markSupported(): Boolean = stream.markSupported()

    override fun read(): Int = stream.read()

    override fun read(b: ByteArray): Int = stream.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int = stream.read(b, off, len)
}