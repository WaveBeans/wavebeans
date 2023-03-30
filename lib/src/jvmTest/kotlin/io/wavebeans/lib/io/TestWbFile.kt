package io.wavebeans.lib.io

import io.wavebeans.fs.core.WbFile
import io.wavebeans.lib.URI
import java.io.ByteArrayOutputStream

class TestWbFile(
    val fs: MutableMap<String, ByteArray>,
    override val uri: URI
) : WbFile {

    init {
        fs[uri.toString()] = ByteArray(0)
    }

    override fun exists(): Boolean {
        return fs.containsKey(uri.toString())
    }

    override fun delete(): Boolean {
        return fs.remove(uri.toString()) != null
    }

    override fun createWbFileOutputStream(): OutputStream {

        return object : OutputStream, OutputStreamProvider {

            override val stream: java.io.OutputStream = ByteArrayOutputStream()

            override fun write(byte: Int) {
                stream.write(byte)
            }

            override fun write(buffer: ByteArray) {
                stream.write(buffer)
            }

            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                stream.write(buffer, offset, length)
            }

            override fun flush() {
                stream.flush()
            }

            override fun close() {
                stream.close()
                fs[uri.toString()] = (stream as ByteArrayOutputStream).toByteArray()
            }


        }
    }

    override fun createWbFileInputStream(): InputStream {
        return object : InputStream() {
            private val stream = ByteArrayInputStream(fs.getValue(uri.toString()))
            override fun close() {
                stream.close()
            }

            override fun read(): Int {
                return stream.read()
            }

            override fun read(buf: ByteArray): Int {
                return stream.read(buf)
            }

            override fun read(buf: ByteArray, offset: Int, length: Int): Int {
                return stream.read(buf, offset, length)
            }
        }
    }

}