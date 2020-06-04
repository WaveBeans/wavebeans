package io.wavebeans.execution.distributed

import io.wavebeans.communicator.ExceptionObj
import io.wavebeans.execution.BushKey
import io.wavebeans.execution.JobKey
import io.wavebeans.execution.PodRef
import io.wavebeans.execution.pod.PodKey


enum class FutureStatus {
    IN_PROGRESS,
    DONE,
    CANCELLED,
    FAILED
}

data class JobStatus(
        val jobKey: JobKey,
        val status: FutureStatus,
        val exception: ExceptionObj?
)

data class JobContent(
        val bushKey: BushKey,
        val pods: List<PodRef>
)

data class PlantBushRequest(
        val jobKey: JobKey,
        val jobContent: JobContent,
        val sampleRate: Float
)

data class RegisterBushEndpointsRequest(
        val jobKey: JobKey,
        val bushEndpoints: List<BushEndpoint>
)

data class BushEndpoint(
        val bushKey: BushKey,
        val location: String,
        val pods: List<PodKey>
)