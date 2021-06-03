package io.wavebeans.http.rtsp

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import assertk.catch
import io.wavebeans.http.WbHttpService
import io.wavebeans.tests.findFreePort
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class RtpsServerSpec : Spek({
    describe("Base RECORD flow") {

        val content = "1234567890abcdefghijklmnopqrstuvwxyz".toByteArray()

        val buffer by memoized(CachingMode.TEST) {
            ByteArrayOutputStream()
        }

        val mappingContainer by memoized(CachingMode.TEST) {
            Array<RtpMapping?>(1) { null }
        }

        val bufferHandler = { _: Long, _: RtpMapping, buf: ByteArray ->
            buffer.write(buf)
        }

        val tearDownHandler = { _: Long, mapping: RtpMapping ->
            mappingContainer[0] = mapping
        }

        val port = findFreePort()

        val server by memoized(SCOPE) {
            WbHttpService(port).addHandler(RtpsRecordControllerHandler(bufferHandler, tearDownHandler))
        }

        beforeGroup {
            server.start()
        }

        afterGroup {
            server.close()
        }

        it("should access via designated path") {

            RtspSessionClient("localhost", port, "/test").use { client ->
                assertThat(
                    catch { client.options().get() },
                    "should run OPTIONS"
                ).isNull()

                val formatId = 96
                val channel = 0
                assertThat(
                    catch { client.announce("test track", "L8/44100/1", formatId, channel).get() },
                    "should ANNOUNCE the content"
                ).isNull()

                assertThat(
                    catch { client.setup("record", channel).get() },
                    "should SETUP the session"
                ).isNull()

                assertThat(
                    catch { client.record().get() },
                    "should initiate RECORD of the session"
                ).isNull()

                assertThat(
                    catch { client.streamData(formatId, channel, ByteArrayInputStream(content)) },
                    "should stream data"
                ).isNull()

                assertThat(
                    catch { client.tearDown().get() },
                    "should tear down the session"
                ).isNull()

                assertThat(
                    buffer.toByteArray(),
                    "should have written the content"
                ).isEqualTo(content)

                assertThat(mappingContainer[0], "should have specified mapping").isNotNull().all {
                    prop("encoding") { it.encoding.toLowerCase() }.isEqualTo("l8")
                    prop("clockRate") { it.clockRate }.isEqualTo(44100)
                    prop("encodingParameters") { it.encodingParameters?.toLowerCase() }.isEqualTo("1")
                }
            }
        }
    }
})