package io.wavebeans.fs.core

import java.io.FileOutputStream

class LocalWbFileOutputStream(wbFile: LocalWbFile) : WbFileOutputStream() {

    private val stream = FileOutputStream(wbFile.file)

    override fun write(b: Int) = stream.write(b)

    override fun write(b: ByteArray) = stream.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)

    override fun flush() = stream.flush()

    override fun close() = stream.close()
}