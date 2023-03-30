package io.wavebeans.fs.local

import io.wavebeans.lib.io.OutputStream
import io.wavebeans.lib.io.OutputStreamProvider
import java.io.FileOutputStream

class LocalWbFileOutputStream(wbFile: LocalWbFile) : OutputStream, OutputStreamProvider {

    override val stream = FileOutputStream(wbFile.file)

    override fun write(b: Int) = stream.write(b)

    override fun write(b: ByteArray) = stream.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)

    override fun flush() = stream.flush()

    override fun close() = stream.close()
}