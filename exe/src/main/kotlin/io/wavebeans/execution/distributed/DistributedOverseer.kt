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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
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
                val bushEndpoints = mutableListOf<BushEndpoint>()
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

                    bushEndpoints += BushEndpoint(bushKey, location, pods.map { it.key })
                }

                // register bush endpoints from other crew gardeners
                val byLocation = bushEndpoints.groupBy { it.url }
                byLocation.keys.forEach { location ->
                    val request = RegisterBushEndpointsRequest(
                            byLocation.filterKeys { it != location }
                                    .map { it.value }
                                    .flatten()
                    )

                    val response = client.post<HttpResponse>("$location/bush/endpoints") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        body = json.stringify(RegisterBushEndpointsRequest.serializer(), request)
                    }

                    if (response.status != HttpStatusCode.OK)
                        throw IllegalStateException("Can't register bush endpoints: $request. Crew Gardener on $location " +
                                "returned non 200 HTTP code. Response: $response ")
                }

                // start the job for all crew gardeners
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

