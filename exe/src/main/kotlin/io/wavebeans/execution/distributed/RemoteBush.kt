package io.wavebeans.execution.distributed

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
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
            client.get<HttpStatement>("$endpoint/bush/call?bushKey=$bushKey&podId=${podKey.id}" +
                    "&podPartition=${podKey.partition}&request=${request.encodeURLParameter()}"
            ).execute { response ->
                val istream = response.receive<ByteReadChannel>().toInputStream()
                future.complete(ExecutionConfig.podCallResultBuilder().fromInputStream(istream))
            }
        }
        return future
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}