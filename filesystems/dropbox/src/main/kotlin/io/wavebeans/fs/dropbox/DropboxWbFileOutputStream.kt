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

    private val session = client.files().uploadSessionStart().finish()
    private val buffer = ByteArray(dropboxDriverConfig.bufferSize)
    private var count = 0
    private var offset = 0L

    override fun write(byte: Int) {
        buffer[count] = byte.toByte()
        if (++count == buffer.size) {
            flush()
        }
    }

    override fun write(buffer: ByteArray) {
        write(buffer, 0, buffer.size)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset + length > buffer.size) {
            throw IndexOutOfBoundsException("offset=$offset, length=$length, size=${buffer.size}")
        }
        if (length == 0) return

        var srcPos = offset
        var remaining = length
        while (remaining > 0) {
            // if internal buffer is full, flush it first
            if (count == this.buffer.size) {
                flush()
            }
            val space = this.buffer.size - count
            val toCopy = minOf(remaining, space)
            System.arraycopy(buffer, srcPos, this.buffer, count, toCopy)
            count += toCopy
            srcPos += toCopy
            remaining -= toCopy
            // flush if filled up to avoid large in-memory accumulation
            if (count == this.buffer.size) {
                flush()
            }
        }
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