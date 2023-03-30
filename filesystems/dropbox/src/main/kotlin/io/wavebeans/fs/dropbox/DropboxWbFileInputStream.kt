package io.wavebeans.fs.dropbox

import com.dropbox.core.v2.DbxClientV2
import io.wavebeans.lib.io.InputStream
import java.io.BufferedInputStream

class DropboxWbFileInputStream(
        client: DbxClientV2,
        file: DropboxWbFile,
        private val dropboxDriverConfig: DropboxDriverConfig
) : InputStream() {

    private val stream = BufferedInputStream(client.files().download(file.uri.path).inputStream, dropboxDriverConfig.bufferSize)

    override fun read(): Int = stream.read()

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun read(buf: ByteArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }
}