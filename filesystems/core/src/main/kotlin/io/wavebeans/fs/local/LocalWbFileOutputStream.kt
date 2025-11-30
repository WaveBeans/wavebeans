package io.wavebeans.fs.local

import io.wavebeans.lib.io.OutputStream
import io.wavebeans.lib.io.OutputStreamProvider
import java.io.FileOutputStream

class LocalWbFileOutputStream(wbFile: LocalWbFile) : OutputStream, OutputStreamProvider {

    override val stream = FileOutputStream(wbFile.file)

    override fun write(byte: Int) = stream.write(byte)

    override fun write(buffer: ByteArray) = stream.write(buffer)

    override fun write(buffer: ByteArray, offset: Int, length: Int) = stream.write(buffer, offset, length)

    override fun flush() = stream.flush()

    override fun close() = stream.close()
}