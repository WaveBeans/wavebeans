package io.wavebeans.fs.dropbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import io.wavebeans.fs.core.WbFileDriver
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.URI

object DropboxWbFileSpec : Spek({

    afterEachGroup {
        WbFileDriver.unregisterDriver("dropbox")
    }

    describe("Using generated access token") {

        beforeGroup {
            DropboxWbFileDriver.configure(
                    System.getenv("DBX_TEST_CLIENT_ID"),
                    System.getenv("DBX_TEST_ACCESS_TOKEN")
            )
        }

        it("should create temporary file, then write, read and delete it in default temp directory") {
            val file = WbFileDriver.instance("dropbox").createTemporaryWbFile("test", "txt")

            file.createWbFileOutputStream().use {
                it.write("abcdefg".toByteArray())
            }

            val s = file.createWbFileInputStream().bufferedReader().use {
                it.readLine()
            }

            assertThat(s).isEqualTo("abcdefg")

            file.delete()

            assertThat(file.exists()).isFalse()
        }

        it("should create temporary file, then write, read and delete it in custom temp directory") {
            val tmpDirectory = WbFileDriver.instance("dropbox").createWbFile(URI("dropbox:///myTmp/folder"))
            val file = WbFileDriver.instance("dropbox").createTemporaryWbFile("test", "txt", tmpDirectory)

            file.createWbFileOutputStream().use {
                it.write("abcdefg".toByteArray())
            }

            val s = file.createWbFileInputStream().bufferedReader().use {
                it.readLine()
            }

            assertThat(s).isEqualTo("abcdefg")

            file.delete()

            assertThat(file.exists()).isFalse()
        }
    }
})