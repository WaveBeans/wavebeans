package io.wavebeans.fs.dropbox

import com.dropbox.core.v2.DbxClientV2
import io.wavebeans.fs.core.WbFileInputStream
import java.io.BufferedInputStream

class DropboxWbFileInputStream(
        client: DbxClientV2,
        file: DropboxWbFile,
        private val dropboxDriverConfig: DropboxDriverConfig
) : WbFileInputStream() {

    private val stream = BufferedInputStream(client.files().download(file.uri.path).inputStream, dropboxDriverConfig.bufferSize)

    override fun read(): Int = stream.read()
}