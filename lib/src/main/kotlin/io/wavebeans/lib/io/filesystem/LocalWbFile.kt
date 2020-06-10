package io.wavebeans.lib.io.filesystem

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

object LocalWbFileFactory : WbFileFactory {

    override fun createTemporaryWbFile(prefix: String, suffix: String, parent: WbFile?): WbFile {
        return if (parent == null)
            LocalWbFile(File.createTempFile(prefix, suffix))
        else if (parent is LocalWbFile)
            LocalWbFile(File.createTempFile(prefix, suffix, parent.file))
        else
            throw IllegalArgumentException("Can't create a file with parent of different type: $parent")
    }


    override fun createWbFile(uri: URI): WbFile = LocalWbFile(File(uri))

}

class LocalWbFile(val file: File) : WbFile {
    override fun exists(): Boolean = file.exists()

    override fun delete(): Boolean = file.delete()

    override fun createWbFileOutputStream(): WbFileOutputStream = LocalWbFileOutputStream(this)

    override fun createWbFileInputStream(): WbFileInputStream = LocalWbFileInputStream(this)
}

class LocalWbFileOutputStream(wbFile: LocalWbFile) : WbFileOutputStream() {

    private val stream = FileOutputStream(wbFile.file)

    override fun write(b: Int) = stream.write(b)

    override fun write(b: ByteArray) = stream.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)

    override fun flush() = stream.flush()

    override fun close() = stream.close()
}

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