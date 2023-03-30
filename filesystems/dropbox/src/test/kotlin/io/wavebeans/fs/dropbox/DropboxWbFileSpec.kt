package io.wavebeans.fs.dropbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.URI
import io.wavebeans.lib.io.bufferedReader
import io.wavebeans.lib.io.use
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DropboxWbFileSpec : Spek({

    afterEachGroup {
        WbFileDriver.unregisterDriver("dropbox")
    }

    describe("Using generated access token") {

        beforeGroup {
            DropboxWbFileDriver.configure(
                    clientIdentifier = System.getenv("DBX_TEST_CLIENT_ID"),
                    accessToken = System.getenv("DBX_TEST_ACCESS_TOKEN"),
                    bufferSize = 2
            )
        }

        val fileContent = "abcdefghigjklmnopqrstuvwxyz"
        it("should create temporary file, then write, read and delete it in default temp directory") {
            val file = WbFileDriver.instance("dropbox").createTemporaryWbFile("test", "txt")

            file.createWbFileOutputStream().use {
                it.write(fileContent.toByteArray())
            }

            val s = file.createWbFileInputStream().bufferedReader().use {
                it.readLine()
            }

            assertThat(s).isEqualTo(fileContent)

            file.delete()

            assertThat(file.exists()).isFalse()
        }

        it("should create temporary file, then write, read and delete it in custom temp directory") {
            val tmpDirectory = WbFileDriver.instance("dropbox").createWbFile(URI("dropbox:///myTmp/folder"))
            val file = WbFileDriver.instance("dropbox").createTemporaryWbFile("test", "txt", tmpDirectory)

            file.createWbFileOutputStream().use {
                it.write(fileContent.toByteArray())
            }

            val s = file.createWbFileInputStream().bufferedReader().use {
                it.readLine()
            }

            assertThat(s).isEqualTo(fileContent)

            file.delete()

            assertThat(file.exists()).isFalse()
        }
    }
})