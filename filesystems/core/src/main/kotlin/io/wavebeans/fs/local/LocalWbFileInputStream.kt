package io.wavebeans.fs.local

import io.wavebeans.lib.io.InputStream
import io.wavebeans.lib.io.InputStreamProvider
import java.io.FileInputStream

class LocalWbFileInputStream(wbFile: LocalWbFile) : InputStream, InputStreamProvider {

    override val stream = FileInputStream(wbFile.file)

    override fun read(): Int = stream.read()

    override fun read(buf: ByteArray): Int = stream.read(buf)

    override fun read(buf: ByteArray, offset: Int, length: Int): Int = stream.read(buf, offset, length)

    override fun close() {
        stream.close()
    }
}