package io.wavebeans.fs.local

import io.wavebeans.lib.io.InputStream
import java.io.FileInputStream

class LocalWbFileInputStream(wbFile: LocalWbFile) : InputStream {

    private val stream = FileInputStream(wbFile.file)

//    override fun skip(n: Long): Long = stream.skip(n)
//
//    override fun available(): Int = stream.available()
//
//    override fun reset() = stream.reset()
//
//    override fun close() = stream.close()
//
//    override fun mark(readlimit: Int) = stream.mark(readlimit)
//
//    override fun markSupported(): Boolean = stream.markSupported()

    override fun read(): Int = stream.read()

    override fun read(buf: ByteArray): Int = stream.read(buf)

    override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)

    override fun close() {
        stream.close()
    }
}