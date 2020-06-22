package io.wavebeans.fs.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import io.wavebeans.fs.core.WbFile
import io.wavebeans.fs.core.WbFileDriver
import mu.KotlinLogging
import java.net.URI
import kotlin.random.Random

class DropboxWbFileDriver(
        clientIdentifier: String,
        accessToken: String,
        private val defaultTemporaryDirectory: String = "/tmp"
) : WbFileDriver {

    companion object {
        private val log = KotlinLogging.logger { }

        fun configure(
                clientIdentifier: String,
                accessToken: String,
                scheme: String = "dropbox",
                defaultTemporaryDirectory: String = "/tmp"
        ) {
            WbFileDriver.registerDriver(scheme, DropboxWbFileDriver(clientIdentifier, accessToken, defaultTemporaryDirectory))
        }
    }

    private val dropboxClient: DbxClientV2 by lazy {
        val config = DbxRequestConfig.newBuilder(clientIdentifier)
                .build()
        val dropboxClient = DbxClientV2(config, accessToken)

        log.info {
            val account = dropboxClient.users().currentAccount!!
            "Logged in as ${account.name.displayName}, type=${account.accountType}"
        }
        dropboxClient
    }

    override fun createTemporaryWbFile(prefix: String, suffix: String, parent: WbFile?): WbFile {
        require(parent == null || parent is DropboxWbFile) { "$parent can only be instance of ${DropboxWbFile::class}" }
        log.trace { "Creating temporary file prefix=$prefix, suffix=$suffix, parent=$parent" }
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()
        val directory = parent?.let { (it as DropboxWbFile).uri.path } ?: defaultTemporaryDirectory
        var file: DropboxWbFile?
        var attempts = 5
        do {
            val rnd = (0..5).map { alphabet[Random.nextInt(alphabet.size)] }.joinToString("")
            file = DropboxWbFile(dropboxClient, URI("dropbox://$directory/$prefix.$rnd.$suffix"))
            if (!file.exists()) break else file = null
        } while (file == null && --attempts > 0)
        log.trace { "Temporary file is $file" }
        return file ?: throw IllegalStateException("Can't create temporary file")
    }


    override fun createWbFile(uri: URI): WbFile = DropboxWbFile(dropboxClient, uri)

}