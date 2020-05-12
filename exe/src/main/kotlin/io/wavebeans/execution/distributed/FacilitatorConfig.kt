package io.wavebeans.execution.distributed

import com.uchuhimo.konf.ConfigSpec

object FacilitatorConfig : ConfigSpec() {
    val advertisingHostAddress by optional("127.0.0.1", name = "advertisingHostAddress", description = "The name the Facilitator is advertised under")
    val listeningPortRange by required<IntRange>(name = "listeningPortRange", description = "The range facilitator will choose the port randomly from. Whichever won't be occupied")
    val startingUpAttemptsCount by optional(10, name = "startingUpAttemptsCount", description = "The number of attempts the facilitator will try to make to bind to port")
    val threadsNumber by required<Int>(name = "threadsNumber", description = "The capacity of working pool for this facilitator. It is going to be shared across all jobs")
    val callTimeoutMillis by optional(5000L, name = "callTimeoutMillis", description = "The maximum time the facilitator will wait for the answer from the pod, in milliseconds")
    val onServerShutdownGracePeriodMillis by optional(5000L, name = "onServerShutdownGracePeriodMillis", description = "The time to allow for the HTTP server to gracefully shutdown all resources")
    val onServerShutdownTimeoutMillis by optional(5000L, name = "onServerShutdownTimeoutMillis", description = "The time to wait before killing the HTTP server even if it doesn't confirm that")
}