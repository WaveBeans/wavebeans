package io.wavebeans.execution.distributed

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.medium.value
import io.wavebeans.execution.pod.PodKey
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

class RemoteBushSpec : Spek({
    describe("Calling Remote bush") {
        val gardener = mock<Gardener>()
        val jobKey = newJobKey()
        val bushKey = newBushKey()
        val remoteBush = RemoteBush(
                bushKey,
                "http://127.0.0.1:40000"
        )

        // mock pod call result serialization
        val podCallResult = mock<PodCallResult>()
        whenever(podCallResult.writeTo(any()))
                .then { (it.arguments[0] as OutputStream).write("42".toByteArray()) }
        val podCallResultBuilder = mock<PodCallResultBuilder>()
        whenever(podCallResultBuilder.fromInputStream(any()))
                .then {
                    val r = mock<PodCallResult>()
                    val value = String((it.arguments[0] as InputStream).readBytes())
                    whenever(r.value(eq(String::class))).thenReturn(value)
                    r
                }

        // mock bush to return mocked pod call result
        val bush = mock<LocalBush>()
        whenever(bush.call(eq(PodKey(0, 0)), eq("/answer")))
                .then {
                    val completableFuture = CompletableFuture<PodCallResult>()
                    completableFuture.complete(podCallResult)
                    completableFuture
                }

        whenever(gardener.plantBush(eq(jobKey), eq(bushKey), any(), any()))
                .then { PodDiscovery.default.registerBush(bushKey, bush); gardener }

        val crewGardener = CrewGardener(
                advertisingHostAddress = "127.0.0.1",
                listeningPortRange = 40000..40000,
                startingUpAttemptsCount = 1,
                threadsNumber = 1,
                gardener = gardener,
                onServerShutdownGracePeriodMillis = 100,
                onServerShutdownTimeoutMillis = 100,
                podCallResultBuilder = podCallResultBuilder
        )

        beforeGroup {
            crewGardener.start(waitAndClose = false)
            crewGardener.plantBush(PlantBushRequest(jobKey, JobContent(bushKey, listOf(mock())), 44100.0f))
        }

        afterGroup {
            crewGardener.terminate()
            crewGardener.close()
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

})