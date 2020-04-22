package io.wavebeans.execution.distributed

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.wavebeans.execution.*
import io.wavebeans.lib.io.StreamOutput
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import java.util.concurrent.Future

class DistributedOverseer(
        override val outputs: List<StreamOutput<out Any>>,
        private val crewGardenersLocations: List<String>,
        partitionsCount: Int
) : Overseer {

    private val topology = outputs.buildTopology()
            .partition(partitionsCount)
            .groupBeans()


    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {

        val distribution = DistributionPlanner(topology.buildPods(), crewGardenersLocations).distribute()

        val jobKey = newJobKey()
        val json = Json(JsonConfiguration.Stable, TopologySerializer.paramsModule)

        HttpClient(Apache).use { client ->
            runBlocking {
                distribution.entries.forEach { (location, pods) ->
                    // for now assume the code is accessible
                    // client.post("$location/code/upload")
                    // plant bush
                    val bushKey = newBushKey()
                    val response = client.post<HttpResponse>("$location/bush") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        val bushContent = JobContent(bushKey, pods)
                        val plantBushRequest = PlantBushRequest(jobKey, bushContent, sampleRate)
                        body = json.stringify(PlantBushRequest.serializer(), plantBushRequest)
                    }
                    if (response.status != HttpStatusCode.OK)
                        throw IllegalStateException("Can't distribute pods for job $jobKey. Crew Gardener on $location " +
                                "returned non 200 HTTP code. Response: $response ")
                }

                distribution.keys.forEach { location ->
                    val response = client.put<HttpResponse>("$location/job?jobKey=$jobKey")
                    if (response.status != HttpStatusCode.OK)
                        throw IllegalStateException("Can start job $jobKey. Crew Gardener on $location " +
                                "returned non 200 HTTP code. Response: $response ")
                }
            }
        }
        return emptyList()
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}

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