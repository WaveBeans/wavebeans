package io.wavebeans.fs.dropbox

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.fileproperties.PropertyGroup
import com.dropbox.core.v2.files.CommitInfo
import com.dropbox.core.v2.files.UploadSessionCursor
import com.dropbox.core.v2.files.WriteMode
import io.wavebeans.lib.io.OutputStream
import java.io.ByteArrayInputStream
import java.util.*

class DropboxWbFileOutputStream(
        val client: DbxClientV2,
        val file: DropboxWbFile,
        private val dropboxDriverConfig: DropboxDriverConfig
) : OutputStream {

    private val session = client.files().uploadSessionStart(false).finish()
    private val buffer = ByteArray(dropboxDriverConfig.bufferSize)
    private var count = 0
    private var offset = 0L

    override fun write(b: Int) {
        buffer[count] = b.toByte()
        if (++count == buffer.size) {
            flush()
        }
    }

    override fun write(buffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        client.files().uploadSessionAppendV2(
                UploadSessionCursor(
                        session.sessionId,
                        offset
                )
        ).uploadAndFinish(ByteArrayInputStream(buffer, 0, count))
        offset += count
        count = 0
    }

    override fun close() {
        flush()
        client.files().uploadSessionFinish(
                UploadSessionCursor(
                        session.sessionId,
                        offset
                ),
                CommitInfo(
                        file.uri.path,
                        WriteMode.OVERWRITE,
                        false,
                        Date(),
                        false,
                        emptyList<PropertyGroup>(),
                        false
                )
        ).finish()
    }
}