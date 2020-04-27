package io.wavebeans.execution.distributed

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.wavebeans.execution.Bush
import io.wavebeans.execution.BushKey
import io.wavebeans.execution.ExecutionResult
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class RemoteBush(
        override val bushKey: BushKey,
        val endpoint: String
) : Bush {

    private val client = HttpClient(Apache)

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
        val future = CompletableFuture<PodCallResult>()
        runBlocking {
            val req = "$endpoint/bush/call?bushKey=$bushKey&podId=${podKey.id}" +
                    "&podPartition=${podKey.partition}&request=${request.encodeURLParameter()}"
            client.get<HttpStatement>(req).execute { response ->
                try {
                    if (response.status == HttpStatusCode.OK) {
                        val istream = response.receive<ByteReadChannel>().toInputStream()
                        future.complete(ExecutionConfig.podCallResultBuilder().fromInputStream(istream))
                    } else {
                        future.completeExceptionally(IllegalStateException("Non 200 code response during request: $req. Response: $response"))
                    }
                } catch (e: Throwable) {
                    future.completeExceptionally(IllegalStateException("Unexpected error during request: $req. Response: $response", e))
                }
            }
        }
        return future
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}