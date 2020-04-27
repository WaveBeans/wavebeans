package io.wavebeans.execution.distributed

import io.wavebeans.execution.PodRef
import io.wavebeans.execution.TopologySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging

class DistributionPlanner(
        private val pods: List<PodRef>,
        private val crewGardenersLocations: List<String>
) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    fun distribute(): Map<String, List<PodRef>> {
        val minimumCountPerLocation = pods.size / crewGardenersLocations.size
        var additionalPodsCount = pods.size % crewGardenersLocations.size
        val podsLeft = pods.toMutableList()

        return crewGardenersLocations.map {
            val count = minimumCountPerLocation + if (additionalPodsCount-- > 0) 1 else 0
            val locationPods = (0 until count).map { podsLeft.removeFirst() }
            it to locationPods
        }.toMap().also {
            log.info {
                val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true), TopologySerializer.paramsModule)
                "Planned the following distribution:\n" +
                        it.entries.joinToString("\n") {
                            "${it.key} -> ${json.stringify(ListSerializer(PodRef.serializer()), it.value)}"
                        }
            }
        }
    }
}