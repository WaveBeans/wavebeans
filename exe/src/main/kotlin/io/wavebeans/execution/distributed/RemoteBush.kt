package io.wavebeans.execution.distributed

import io.wavebeans.communicator.FacilitatorApiClient
import io.wavebeans.execution.Bush
import io.wavebeans.execution.BushKey
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.PodKey
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RemoteBush(
        override val bushKey: BushKey,
        val endpoint: String
) : Bush {

    private val facilitatorApiClient = FacilitatorApiClient(endpoint)

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun start() {}

    override fun call(podKey: PodKey, request: String): Future<PodCallResult> {
        val req = "$endpoint/bush/call?bushKey=$bushKey&podId=${podKey.id}&podPartition=${podKey.partition}&request=$request"
        log.trace { "Making bush call $req" }
        val future = CompletableFuture<PodCallResult>()
        try {
            val response = facilitatorApiClient.call(bushKey, podKey.id, podKey.partition, request)
            future.complete(ExecutionConfig.podCallResultBuilder().fromInputStream(response))
        } catch (e: Throwable) {
            if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
            future.completeExceptionally(IllegalStateException("Unexpected error during request: $req.", e))
        }

        return future
    }

    override fun close() {
        log.info { "Remote bush $bushKey pointing to $endpoint is closing" }
        facilitatorApiClient.close()
    }

}