package io.wavebeans.execution.distributed

import io.wavebeans.execution.BushKey
import io.wavebeans.execution.JobKey
import io.wavebeans.execution.PodRef
import io.wavebeans.execution.UUIDSerializer
import io.wavebeans.execution.pod.PodKey
import kotlinx.serialization.Serializable


enum class FutureStatus {
    IN_PROGRESS,
    DONE,
    CANCELLED,
    FAILED
}

@Serializable
data class JobStatus(
        @Serializable(with = UUIDSerializer::class)
        val jobKey: JobKey,
        val status: FutureStatus,
        val exception: ExceptionObj?
)

@Serializable
data class JobContent(
        @Serializable(with = UUIDSerializer::class)
        val bushKey: BushKey,
        val pods: List<PodRef>
)

@Serializable
data class PlantBushRequest(
        @Serializable(with = UUIDSerializer::class)
        val jobKey: JobKey,
        val jobContent: JobContent,
        val sampleRate: Float
)

@Serializable
data class RegisterBushEndpointsRequest(
        @Serializable(with = UUIDSerializer::class)
        val jobKey: JobKey,
        val bushEndpoints: List<BushEndpoint>
)

@Serializable
data class BushEndpoint(
        @Serializable(with = UUIDSerializer::class)
        val bushKey: BushKey,
        val location: String,
        val pods: List<PodKey>
)