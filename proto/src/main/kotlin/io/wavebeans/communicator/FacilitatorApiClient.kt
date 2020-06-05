package io.wavebeans.communicator

import com.google.protobuf.ByteString
import io.grpc.Channel
import java.io.InputStream
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.MILLISECONDS


class FacilitatorApiClient(
        location: String,
        override val onCloseTimeoutMs: Long = 5000
) : AbstractClient(location) {

    private lateinit var client: FacilitatorApiGrpc.FacilitatorApiFutureStub
    private lateinit var blockingClient: FacilitatorApiGrpc.FacilitatorApiBlockingStub

    override fun createClient(channel: Channel) {
        client = FacilitatorApiGrpc.newFutureStub(channel)
        blockingClient = FacilitatorApiGrpc.newBlockingStub(channel)
    }

    fun call(bushKey: UUID, podId: Long, podPartition: Int, request: String): InputStream {
        val iterator = blockingClient.call(
                CallRequest.newBuilder()
                        .setBushKey(bushKey.toString())
                        .setPodId(podId)
                        .setPodPartition(podPartition)
                        .setRequest(request)
                        .build()
        ).iterator()
        return object : InputStream() {

            var buffer: ByteArray? = null
            var bufferPointer = 0

            override fun read(): Int {
                return when {
                    (buffer == null || bufferPointer >= buffer!!.size) && !iterator.hasNext() -> -1
                    (buffer == null || bufferPointer >= buffer!!.size) && iterator.hasNext() -> {
                        buffer = iterator.next().buffer.toByteArray()
                        bufferPointer = 0
                        buffer!![bufferPointer++].toInt() and 0xFF
                    }
                    buffer != null && bufferPointer < buffer!!.size -> buffer!![bufferPointer++].toInt() and 0xFF
                    else -> throw UnsupportedOperationException("unreachable branch: buffer=$buffer, " +
                            "buffer.size=${buffer?.size}, iterator.hasNext=${iterator.hasNext()}")
                }
            }
        }
    }

    fun terminate(timeoutMs: Long = 5000) {
        client.terminate(
                TerminateRequest.getDefaultInstance()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun startJob(jobKey: UUID, timeoutMs: Long = 5000) {
        client.startJob(
                StartJobRequest.newBuilder()
                        .setJobKey(jobKey.toString())
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun stopJob(jobKey: UUID, timeoutMs: Long = 5000) {
        client.stopJob(
                StopJobRequest.newBuilder()
                        .setJobKey(jobKey.toString())
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun jobStatus(jobKey: UUID, timeoutMs: Long = 5000): JobStatusResponse {
        return client.jobStatus(JobStatusRequest.newBuilder()
                .setJobKey(jobKey.toString())
                .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun describeJob(jobKey: UUID, timeoutMs: Long = 5000): List<JobContent> {
        return client.describeJob(
                DescribeJobRequest.newBuilder()
                        .setJobKey(jobKey.toString())
                        .build()
        )
                .get(timeoutMs, MILLISECONDS)
                .jobContentList
    }

    fun listJobs(timeoutMs: Long = 5000): List<UUID> {
        return client.listJobs(ListJobsRequest.getDefaultInstance())
                .get(timeoutMs, MILLISECONDS)
                .jobKeysList
                .map { UUID.fromString(it) }
    }

    fun uploadCode(jobKey: UUID, jarFileContentStream: InputStream, timeoutMs: Long = 5000) {
        client.uploadCode(
                UploadCodeRequest.newBuilder()
                        .setJobKey(jobKey.toString())
                        .setJarFileContent(ByteString.copyFrom(jarFileContentStream.readBytes()))
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }


    fun codeClasses(): Sequence<CodeClassesPartialResponse> {
        return blockingClient.codeClasses(
                CodeClassesRequest.getDefaultInstance()
        ).iterator().asSequence()
    }

    fun plantBush(jobKey: UUID, bushKey: UUID, sampleRate: Float, podsAsJson: String, timeoutMs: Long = 5000) {
        client.plantBush(
                PlantBushRequest.newBuilder()
                        .setJobKey(jobKey.toString())
                        .setSampleRate(sampleRate)
                        .setJobContent(
                                JobContent.newBuilder()
                                        .setBushKey(bushKey.toString())
                                        .setPodsAsJson(podsAsJson)
                                        .build()
                        )
                        .build()
        ).get(timeoutMs, MILLISECONDS)
    }

    fun registerBushEndpoints(request: RegisterBushEndpointsRequest, timeoutMs: Long = 5000) {
        client.registerBushEndpoints(request).get(timeoutMs, MILLISECONDS)
    }

    fun status(timeoutMs: Long = 5000): Boolean {
        return try {
            client.status(
                    StatusRequest.getDefaultInstance()
            ).get(timeoutMs, MILLISECONDS)
            true
        } catch (e: ExecutionException) {
            if (e.cause is io.grpc.StatusRuntimeException)
                false
            else
                throw e
        }
    }
}