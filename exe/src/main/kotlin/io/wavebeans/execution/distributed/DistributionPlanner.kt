package io.wavebeans.execution.distributed

import io.wavebeans.execution.PodRef
import io.wavebeans.execution.TopologySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging

interface DistributionPlanner {
    fun distribute(pods: List<PodRef>, crewGardenersLocations: List<String>): Map<String, List<PodRef>>
}

class EvenDistributionPlanner : DistributionPlanner {

    override fun distribute(pods: List<PodRef>, crewGardenersLocations: List<String>): Map<String, List<PodRef>> {
        val minimumCountPerLocation = pods.size / crewGardenersLocations.size
        var additionalPodsCount = pods.size % crewGardenersLocations.size
        val podsLeft = pods.toMutableList()

        return crewGardenersLocations.map {
            val count = minimumCountPerLocation + if (additionalPodsCount-- > 0) 1 else 0
            val locationPods = (0 until count).map { podsLeft.removeFirst() }
            it to locationPods
        }.toMap()
    }
}