package io.wavebeans.execution.distributed

import io.wavebeans.execution.PodRef

interface DistributionPlanner {
    fun distribute(pods: List<PodRef>, facilitatorLocations: List<String>): Map<String, List<PodRef>>
}

class EvenDistributionPlanner : DistributionPlanner {

    override fun distribute(pods: List<PodRef>, facilitatorLocations: List<String>): Map<String, List<PodRef>> {
        val minimumCountPerLocation = pods.size / facilitatorLocations.size
        var additionalPodsCount = pods.size % facilitatorLocations.size
        val podsLeft = pods.toMutableList()

        return facilitatorLocations.map {
            val count = minimumCountPerLocation + if (additionalPodsCount-- > 0) 1 else 0
            val locationPods = (0 until count).map { podsLeft.removeFirst() }
            it to locationPods
        }.toMap()
    }
}