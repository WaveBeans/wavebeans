package io.wavebeans.fs.dropbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.fs.core.WbFileDriver
import java.net.URI

@OptIn(ExperimentalKotest::class)
class DropboxWbFileSpec : DescribeSpec({

    afterTest {
        WbFileDriver.unregisterDriver("dropbox")
    }

    val clientIdentifier = System.getenv("DBX_TEST_CLIENT_ID")
    val accessToken = System.getenv("DBX_TEST_ACCESS_TOKEN")

    describe("Using generated access token")
        .config(enabledIf = { !clientIdentifier.isNullOrBlank() && !accessToken.isNullOrBlank() }) {

            beforeTest {
                DropboxWbFileDriver.configure(
                    clientIdentifier = clientIdentifier,
                    accessToken = accessToken,
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
                val tmpDirectory =
                    WbFileDriver.instance("dropbox").createWbFile(URI("dropbox:///myTmp/folder"))
                val file =
                    WbFileDriver.instance("dropbox").createTemporaryWbFile("test", "txt", tmpDirectory)

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