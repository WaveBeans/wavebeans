package io.wavebeans.execution.distributed

import com.uchuhimo.konf.ConfigSpec

object FacilitatorConfig : ConfigSpec() {
    val communicatorPort by required<Int>(name = "communicatorPort", description = "The port the communicator server will start and Facilitator will be reachable for API calls.")
    val threadsNumber by required<Int>(name = "threadsNumber", description = "The capacity of working pool for this facilitator. It is going to be shared across all jobs")
    val callTimeoutMillis by optional(5000L, name = "callTimeoutMillis", description = "The maximum time the facilitator will wait for the answer from the pod, in milliseconds")
    val onServerShutdownTimeoutMillis by optional(5000L, name = "onServerShutdownTimeoutMillis", description = "The time to wait before killing the Communicator server even if it doesn't confirm that")
}