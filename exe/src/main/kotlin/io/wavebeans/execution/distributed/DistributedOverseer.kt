package io.wavebeans.execution.distributed

import io.wavebeans.execution.*
import io.wavebeans.lib.io.StreamOutput
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.concurrent.*

class DistributedOverseer(
        override val outputs: List<StreamOutput<out Any>>,
        private val crewGardenersLocations: List<String>,
        partitionsCount: Int,
        private val distributionPlanner: DistributionPlanner = EvenDistributionPlanner()
) : Overseer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val topology = outputs.buildTopology()
            .partition(partitionsCount)
            .groupBeans()
    private val jobKey = newJobKey()
    private val locationFutures = ConcurrentHashMap<String, CompletableFuture<ExecutionResult>>()
    private val crewGardenerCheckPool = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory("crew-gardener-check"))
    private val distribution by lazy {
        distributionPlanner.distribute(topology.buildPods(), crewGardenersLocations).also {
            log.info {
                val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true), TopologySerializer.paramsModule)
                "Planned the even distribution:\n" +
                        it.entries.joinToString("\n") {
                            "${it.key} -> ${json.stringify(ListSerializer(PodRef.serializer()), it.value)}"
                        }
            }
        }
    }

    private inner class CrewGardenerCheckJob(val location: String) : Runnable {

        private val log = KotlinLogging.logger { }

        private val future = CompletableFuture<ExecutionResult>()

        private val crewGardenerService = CrewGardenerService.create(location)

        fun start() {
            locationFutures[location] = future
            reschedule()
        }

        override fun run() {
            try {
                val result = fetchResult()
                if (result != null) {
                    if (result.exception != null) {
                        log.info(result.exception) { "Completting with exception on $location for job $jobKey" }
                        future.complete(ExecutionResult.error(result.exception))
                    } else {
                        log.info { "Completting successfully on $location for job $jobKey" }
                        future.complete(ExecutionResult.success())
                    }
                } else {
                    reschedule()
                }
            } catch (e: Throwable) {
                log.info(e) { "Failed execution on $location for job $jobKey" }
                future.completeExceptionally(e)
            }
        }

        private fun reschedule() {
            crewGardenerCheckPool.schedule(this, 1000, TimeUnit.MILLISECONDS)
        }

        fun fetchResult(): ExecutionResult? {
            val response = crewGardenerService.jobStatus(jobKey).execute().throwIfError()
            val result = response.body()
                    ?: throw IllegalStateException("Expected non-empty response for request for job status on job $jobKey on $location")
            return when {
                result.all { it.status == FutureStatus.DONE || it.status == FutureStatus.CANCELLED || it.status == FutureStatus.FAILED }
                        && result.any { it.status == FutureStatus.FAILED } -> {
                    log.error { "Errors during job $jobKey execution:\n" + result.mapNotNull { it.exception }.joinToString("\n") }
                    ExecutionResult.error(result.first { it.status == FutureStatus.FAILED }.exception?.toException() ?: IllegalStateException("Unknown error"))
                }
                result.all { it.status == FutureStatus.DONE || it.status == FutureStatus.CANCELLED } -> {
                    ExecutionResult.success()
                }
                else -> null
            }
        }
    }

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {

        val bushEndpoints = plantBushes(distribution, jobKey, sampleRate)
        bushEndpoints.forEach { CrewGardenerCheckJob(it.location).start() }
        registerBushEndpoint(bushEndpoints)
        startJob(distribution.keys, jobKey)

        return locationFutures.values.toList()
    }

    private fun plantBushes(distribution: Map<String, List<PodRef>>, jobKey: JobKey, sampleRate: Float): List<BushEndpoint> {
        val bushEndpoints = mutableListOf<BushEndpoint>()
        distribution.entries.forEach { (location, pods) ->
            val crewGardenerService = CrewGardenerService.create(location)
            // upload code to the crew gardener
            System.getProperty("java.class.path").split(":")
                    .filter { it.endsWith("code.jar") }
                    .forEach { jarFileName ->
                        log.info { "Uploading $jarFileName to $location" }
                        val jarFile = File(jarFileName)
                        val file = RequestBody.create(null, jarFile)
                        val code = MultipartBody.Part.createFormData("code", jarFile.name, file)
                        val jk = RequestBody.create(null, jobKey.toString())
                        crewGardenerService.uploadCode(jk, code).execute().throwIfError()
                    }

            // plant bush
            val bushKey = newBushKey()
            val bushContent = JobContent(bushKey, pods)
            val plantBushRequest = PlantBushRequest(jobKey, bushContent, sampleRate)
            crewGardenerService.plantBush(plantBushRequest).execute().throwIfError()

            bushEndpoints += BushEndpoint(bushKey, location, pods.map { it.key })
        }
        return bushEndpoints
    }

    private fun registerBushEndpoint(bushEndpoints: List<BushEndpoint>) {
        // register bush endpoints from other crew gardeners
        val byLocation = bushEndpoints.groupBy { it.location }
        byLocation.keys.forEach { location ->
            val request = RegisterBushEndpointsRequest(
                    jobKey,
                    byLocation.filterKeys { it != location }
                            .map { it.value }
                            .flatten()
            )
            CrewGardenerService.create(location).registerBushEndpoints(request).execute().throwIfError()
        }
    }

    private fun startJob(locations: Set<String>, jobKey: JobKey) {
        // start the job for all crew gardeners
        locations.forEach { location ->
            CrewGardenerService.create(location).startJob(jobKey).execute().throwIfError()
        }
    }

    override fun close() {
        log.info { "Closing overseer for job $jobKey. Status of futures: $locationFutures" }
        log.info { "Shutting down the Crew Gardener check pool" }
        crewGardenerCheckPool.shutdown()
        if (!crewGardenerCheckPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            crewGardenerCheckPool.shutdownNow()
        }
        distribution.keys.forEach { location ->
            log.info { "Stopping job $jobKey on Crew Gardener $location" }
            CrewGardenerService.create(location).stopJob(jobKey).execute().throwIfError()
        }
        log.info { "Stopping http client" }
    }
}

