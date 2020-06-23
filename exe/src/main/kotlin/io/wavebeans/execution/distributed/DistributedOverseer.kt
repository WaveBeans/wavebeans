package io.wavebeans.execution.distributed

import io.wavebeans.communicator.FacilitatorApiClient
import io.wavebeans.communicator.HttpCommunicatorClient
import io.wavebeans.communicator.JobStatusResponse.JobStatus.FutureStatus.*
import io.wavebeans.communicator.RegisterBushEndpointsRequest
import io.wavebeans.execution.*
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.table.TableOutput
import io.wavebeans.lib.table.TableOutputParams
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.reflect.full.isSubclassOf

class DistributedOverseer(
        override val outputs: List<StreamOutput<out Any>>,
        private val facilitatorLocations: List<String>,
        private val httpLocations: List<String>,
        private val partitionsCount: Int,
        private val distributionPlanner: DistributionPlanner = EvenDistributionPlanner(),
        private val additionalClasses: Map<String, File> = emptyMap(),
        private val ignoreLocations: List<Regex> = emptyList()
) : Overseer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val myClasses = startUpClasses()
            .filter { clazz -> !ignoreLocations.any { it.matches(clazz.location) } }

    private val topology = outputs.buildTopology()
            .partition(partitionsCount)
            .groupBeans()
    private val jobKey = newJobKey()
    private val locationFutures = ConcurrentHashMap<String, CompletableFuture<ExecutionResult>>()
    private val facilitatorsCheckPool = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory("facilitators-check"))
    private val distribution by lazy {
        distributionPlanner.distribute(topology.buildPods(), facilitatorLocations).also {
            log.info {
                val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true), TopologySerializer.paramsModule)
                "Planned the even distribution:\n" +
                        it.entries.joinToString("\n") {
                            "${it.key} -> ${json.stringify(ListSerializer(PodRef.serializer()), it.value)}"
                        }
            }
        }
    }

    private inner class FacilitatorCheckJob(val location: String) : Runnable {

        private val log = KotlinLogging.logger { }

        private val future = CompletableFuture<ExecutionResult>()

        private val facilitatorApiClient = FacilitatorApiClient(location)

        fun start() {
            locationFutures[location] = future
            reschedule()
        }

        override fun run() {
            var needReschedule = false
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
                    needReschedule = true
                }
            } catch (e: Throwable) {
                if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
                log.info(e) { "Failed execution on $location for job $jobKey" }
                future.completeExceptionally(e)
            }
            if (needReschedule) {
                reschedule()
            } else {
                facilitatorApiClient.close()
            }
        }

        private fun reschedule() {
            facilitatorsCheckPool.schedule(this, 1000, TimeUnit.MILLISECONDS)
        }

        fun fetchResult(): ExecutionResult? {
            val response = facilitatorApiClient.jobStatus(jobKey)
            val result = response.statusesList
            return when {
                result.all { it.status == DONE || it.status == CANCELLED || it.status == FAILED }
                        && result.any { it.status == FAILED } -> {
                    log.error {
                        "Errors during job $jobKey execution:\n" + result
                                // compiler failure on 1.4-M2
                                //.mapNotNull { it.exception }
                                .map { it.exception }
                                .filter { it != null }
                                .joinToString("\n")
                    }
                    ExecutionResult.error(
                            result.first { it.status == FAILED }
                                    .exception
                                    ?.toException()
                                    ?: IllegalStateException("Unknown error")
                    )
                }
                result.all { it.status == DONE || it.status == CANCELLED } -> {
                    ExecutionResult.success()
                }
                else -> null
            }
        }
    }

    override fun eval(sampleRate: Float): List<Future<ExecutionResult>> {
        log.info {
            "Starting distributed evaluation with following parameters:\n" +
                    "        outputs=$outputs,\n" +
                    "        facilitatorLocations=$facilitatorLocations,\n" +
                    "        partitionsCount=$partitionsCount,\n" +
                    "        distributionPlanner=$distributionPlanner,\n" +
                    "        additionalClasses=$additionalClasses"
        }
        val bushEndpoints = plantBushes(jobKey, sampleRate)
        bushEndpoints.forEach { FacilitatorCheckJob(it.location).start() }
        registerBushEndpoint(bushEndpoints)
        registerTables(sampleRate)
        startJob(jobKey)

        return locationFutures.values.toList()
    }

    private fun registerTables(sampleRate: Float) {
        val facilitatorToTableNames = distribution.entries.map {
            val tableNames = it.value.asSequence()
                    .map { podRef -> podRef.internalBeans }
                    .flatten()
                    .filter { beanRef -> WaveBeansClassLoader.classForName(beanRef.type).kotlin.isSubclassOf(TableOutput::class) }
                    .map { beanRef -> (beanRef.params as TableOutputParams<*>).tableName }
                    .toList()
            it.key to tableNames
        }
        httpLocations.forEach { httpLocation ->
            HttpCommunicatorClient(httpLocation).use { client ->
                facilitatorToTableNames
                        .flatMap { it.second.map { v -> it.first to v } }
                        .forEach { (facilitatorLocation, tableName) ->
                            client.registerTable(tableName, facilitatorLocation, sampleRate)
                        }
            }
        }
    }

    private fun unregisterTables() {
        val tableNames = distribution.entries.map {
            it.value.asSequence()
                    .map { podRef -> podRef.internalBeans }
                    .flatten()
                    .filter { beanRef -> WaveBeansClassLoader.classForName(beanRef.type).kotlin.isSubclassOf(TableOutput::class) }
                    .map { beanRef -> (beanRef.params as TableOutputParams<*>).tableName }
                    .toList()
        }.flatten()

        httpLocations.forEach { httpLocation ->
            HttpCommunicatorClient(httpLocation).use { client ->
                tableNames.forEach { tableName ->
                    client.unregisterTable(tableName)
                }
            }
        }
    }

    private fun plantBushes(jobKey: JobKey, sampleRate: Float): List<BushEndpoint> {
        val bushEndpoints = mutableListOf<BushEndpoint>()
        distribution.entries
                .forEach { (location, pods) ->
                    FacilitatorApiClient(location).use { facilitatorApiClient ->

                        // upload required code to the facilitator
                        val gardenerCodeClasses = facilitatorApiClient.codeClasses()
                                .map { it.classesList }
                                .flatten()
                                .map { descriptor ->
                                    ClassDesc(
                                            descriptor.location,
                                            descriptor.classPath,
                                            descriptor.crc32,
                                            descriptor.size
                                    )
                                }

                        val classesWithoutLocation = gardenerCodeClasses.asSequence()
                                .map { it.copy(location = "") }
                                .toSet()
                        val absentClasses = myClasses
                                .filter { it.copy(location = "") !in classesWithoutLocation }
                        log.info { "Uploading following classes to facilitator on $location:\n" + absentClasses.joinToString("\n") }

                        // pack all absent classes as single jar file
                        val jarDir = createTempDir("code").also { it.deleteOnExit() }
                        absentClasses
                                .groupBy { it.location }.forEach { (l, classDescList) ->
                                    val loc = File(l)
                                    when {
                                        loc.isDirectory -> {
                                            classDescList.forEach {
                                                File(loc, it.classPath).copyTo(File(jarDir, it.classPath))
                                            }
                                        }
                                        loc.extension == "jar" -> {
                                            val jarFile = JarFile(loc)
                                            jarFile.entries().asSequence()
                                                    .filter { ClassDesc(l, it.name, it.crc, it.size) in classDescList }
                                                    .forEach {
                                                        val outputFile = File(jarDir, it.name)
                                                        outputFile.parentFile.mkdirs()
                                                        outputFile.createNewFile()
                                                        val buf = jarFile.getInputStream(it).readBytes()
                                                        outputFile.writeBytes(buf)
                                                    }
                                        }
                                        else -> throw UnsupportedOperationException("$loc is not supported")
                                    }
                                }
                        additionalClasses.forEach {
                            it.value.copyTo(File(jarDir, it.key))
                        }
                        val jarFile = File(createTempDir("code-jar"), "code.jar")
                        JarOutputStream(FileOutputStream(jarFile)).use { jos ->
                            absentClasses.forEach {
                                try {
                                    val entry = JarEntry(it.classPath)
                                    jos.putNextEntry(entry)
                                    jos.write(File(jarDir, it.classPath).readBytes())
                                    jos.closeEntry()
                                } catch (e: java.util.zip.ZipException) {
                                    // ignore duplicate entries
                                    if (!(e.message ?: "").contains("duplicate entry")) {
                                        log.debug { "$it is ignored as duplicate." }
                                        throw e
                                    }
                                }
                            }
                        }

                        // upload code file
                        facilitatorApiClient.uploadCode(jobKey, jarFile.inputStream())

                        // plant bush
                        val bushKey = newBushKey()
                        facilitatorApiClient.plantBush(
                                jobKey,
                                bushKey,
                                sampleRate,
                                jsonCompact(SerializersModule { beanParams(); tableQuery() }).stringify(ListSerializer(PodRef.serializer()), pods)
                        )

                        bushEndpoints += BushEndpoint(bushKey, location, pods.map { it.key })
                    }
                }
        return bushEndpoints
    }

    private fun registerBushEndpoint(bushEndpoints: List<BushEndpoint>) {
        // register bush endpoints from other facilitators
        val byLocation = bushEndpoints.groupBy { it.location }
        byLocation.keys.forEach { location ->
            val request = io.wavebeans.communicator.RegisterBushEndpointsRequest.newBuilder()
                    .setJobKey(jobKey.toString())
            byLocation.filterKeys { it != location }
                    .map { it.value }
                    .flatten()
                    .forEach { bushEndpoint ->
                        val bushEndpointBldr = io.wavebeans.communicator.RegisterBushEndpointsRequest.BushEndpoint
                                .newBuilder()
                                .setBushKey(bushEndpoint.bushKey.toString())
                                .setLocation(bushEndpoint.location)
                        bushEndpoint.pods.forEach {
                            bushEndpointBldr.addPods(
                                    RegisterBushEndpointsRequest.BushEndpoint.PodKey.newBuilder()
                                            .setId(it.id)
                                            .setPartition(it.partition)
                                            .build()
                            )
                        }

                        request.addBushEndpoints(bushEndpointBldr)
                    }

            FacilitatorApiClient(location).use { it.registerBushEndpoints(request.build()) }
        }
    }

    private fun startJob(jobKey: JobKey) {
        // start the job for all facilitators
        distribution.keys.forEach { location ->
            FacilitatorApiClient(location).use { it.startJob(jobKey) }
        }
    }

    override fun close() {
        log.info { "Closing overseer for job $jobKey. Status of futures: $locationFutures" }
        log.info { "Shutting down the Facilitator check pool" }
        facilitatorsCheckPool.shutdown()
        if (!facilitatorsCheckPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            facilitatorsCheckPool.shutdownNow()
        }
        distribution.keys.forEach { location ->
            log.info { "Stopping job $jobKey on Facilitator $location" }
            FacilitatorApiClient(location).use { it.stopJob(jobKey) }
        }
        log.info { "Stopping http client" }
        unregisterTables()
    }
}

