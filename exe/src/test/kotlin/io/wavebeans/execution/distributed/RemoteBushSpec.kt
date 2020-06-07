package io.wavebeans.execution.distributed

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.wavebeans.execution.Gardener
import io.wavebeans.execution.LocalBush
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.medium.value
import io.wavebeans.execution.newBushKey
import io.wavebeans.execution.pod.PodKey
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class RemoteBushSpec : Spek({
    describe("Calling Remote bush") {
        val gardener = mock<Gardener>()
        val podDiscovery = object : PodDiscovery() {}

        // mock pod call result serialization
        val podCallResult = mock<PodCallResult>()
        whenever(podCallResult.stream())
                .then { ByteArrayInputStream("42".toByteArray()) }
        val podCallResultBuilder = mock<PodCallResultBuilder>()
        whenever(podCallResultBuilder.fromInputStream(any()))
                .then {
                    val r = mock<PodCallResult>()
                    val value = String((it.arguments[0] as InputStream).readBytes())
                    whenever(r.value(eq(String::class))).thenReturn(value)
                    r
                }

        val facilitator = Facilitator(
                communicatorPort = 40000,
                threadsNumber = 1,
                gardener = gardener,
                onServerShutdownTimeoutMillis = 100,
                podCallResultBuilder = podCallResultBuilder,
                podDiscovery = podDiscovery
        )

        beforeGroup {
            facilitator.start()
        }

        afterGroup {
            facilitator.terminate()
            facilitator.close()
        }

        describe("Properly working bush") {
            val bushKey by memoized { newBushKey() }

            val remoteBush by memoized {
                // mock bush to return mocked pod call result
                val bush = mock<LocalBush>()
                whenever(bush.call(eq(PodKey(0, 0)), eq("/answer")))
                        .then { CompletableFuture<PodCallResult>().also { it.complete(podCallResult) } }
                podDiscovery.registerBush(bushKey, bush)

                RemoteBush(bushKey, "127.0.0.1:40000")
            }

            it("should start") {
                assertThat(remoteBush.start()).isEqualTo(Unit)
            }

            it("should make a call") {
                assertThat(remoteBush.call(PodKey(0, 0), "/answer").get())
                        .isNotNull()
                        .prop("value") { it.value<String>() }.isEqualTo("42")
            }
        }

        describe("Pointing to non-existing bush") {
            // no bush
            val bushKey by memoized { newBushKey() }
            val remoteBush by memoized { RemoteBush(bushKey, "127.0.0.1:40000") }

            it("should start") {
                assertThat(remoteBush.start()).isEqualTo(Unit)
            }

            it("should fail on call") {
                assertThat(catch { remoteBush.call(PodKey(0, 0), "/answer").get() })
                        .isNotNull().isInstanceOf(ExecutionException::class)
                        .cause()
                        .isNotNull().isInstanceOf(IllegalStateException::class)
                        .message().isNotNull().contains("Unexpected error during request")

            }
        }
    }
})