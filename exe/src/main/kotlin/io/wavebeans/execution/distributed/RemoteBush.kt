package io.wavebeans.execution.distributed

import io.wavebeans.execution.Bush
import io.wavebeans.execution.BushKey
import io.wavebeans.execution.ExecutionResult
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RemoteBush(
        override val bushKey: BushKey,
        val endpoint: String
) : Bush {

    private val crewGardenerService = CrewGardenerService.create(endpoint)

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun start() {}

    override fun addPod(pod: Pod) {
        TODO()
    }

    override fun pods(): List<Pod> {
        TODO()
    }

    override fun tickPodsFutures(): List<Future<ExecutionResult>> {
        TODO()
    }

    override fun call(podKey: PodKey, request: String): Future<PodCallResult> {
        val req = "$endpoint/bush/call?bushKey=$bushKey&podId=${podKey.id}&podPartition=${podKey.partition}&request=$request"
        log.trace { "Making bush call $req" }
        val future = CompletableFuture<PodCallResult>()
        try {
            val response = crewGardenerService.call(bushKey, podKey.id, podKey.partition, request).execute()
            if (response.code() == 200) {
                val istream = response.body()!!.byteStream()
                future.complete(ExecutionConfig.podCallResultBuilder().fromInputStream(istream))
            } else {
                future.completeExceptionally(IllegalStateException("Non 200 code response during request: $req. Response: $response"))

            }
        } catch (e: Throwable) {
            if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
            future.completeExceptionally(IllegalStateException("Unexpected error during request: $req.", e))
        }

        return future
    }

    override fun close() {
        log.info { "Remote bush $bushKey pointing to $endpoint is closing" }
    }

}