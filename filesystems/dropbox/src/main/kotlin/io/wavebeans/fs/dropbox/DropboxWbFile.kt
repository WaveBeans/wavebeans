package io.wavebeans.fs.dropbox

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DeleteErrorException
import com.dropbox.core.v2.files.GetMetadataErrorException
import io.wavebeans.fs.core.*
import io.wavebeans.lib.URI
import io.wavebeans.lib.io.InputStream
import io.wavebeans.lib.io.OutputStream
import mu.KotlinLogging

data class DropboxWbFile(
    private val client: DbxClientV2,
    override val uri: URI,
    private val dropboxDriverConfig: DropboxDriverConfig
) : WbFile {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun exists(): Boolean {
        log.trace { "Checking if file exists on $uri" }
        return try {
            client.files().getMetadata(uri.path)
            log.trace { "$uri exists" }
            true
        } catch (e: GetMetadataErrorException) {
            log.trace(e) { "$uri not exists" }
            false
        }
    }

    override fun delete(): Boolean {
        log.trace { "Deleting file on $uri" }
        return try {
            client.files().deleteV2(uri.path)
            true
        } catch (e: DeleteErrorException) {
            log.warn(e) { "Can't delete file on $uri" }
            false
        }
    }

    override fun createWbFileOutputStream(): OutputStream = DropboxWbFileOutputStream(client, this, dropboxDriverConfig)
            .also { log.trace { "Initialize output stream to $uri" } }

    override fun createWbFileInputStream(): InputStream = DropboxWbFileInputStream(client, this, dropboxDriverConfig)
            .also { log.trace { "Initialize input stream from $uri" } }

    override fun toString(): String {
        return "DropboxWbFile(uri=$uri)"
    }
}

