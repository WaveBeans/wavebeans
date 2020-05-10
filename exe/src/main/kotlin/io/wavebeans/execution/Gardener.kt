package io.wavebeans.execution

import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future

typealias JobKey = UUID

fun newJobKey(): JobKey = UUID.randomUUID()
fun String.toJobKey(): JobKey = UUID.fromString(this)

data class JobDescriptor(
        val bushKey: BushKey,
        val podRefs: List<PodRef>,
        val sampleRate: Float,
        val bush: LocalBush
)

class Gardener {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val activeJobs = ConcurrentHashMap<JobKey, MutableList<JobDescriptor>>()

    fun plantBush(jobKey: JobKey, bushKey: BushKey, pods: List<PodRef>, sampleRate: Float): Gardener {
        val bush = LocalBush(bushKey)
        pods.forEach { bush.addPod(it.instantiate(sampleRate)) }
        val jobDescriptor = JobDescriptor(bushKey, pods, sampleRate, bush)
        activeJobs.putIfAbsent(jobKey, CopyOnWriteArrayList())
        activeJobs.getValue(jobKey).add(jobDescriptor)
        log.info { "JOB[$jobKey] New Bush added with pods=${pods} with sampleRate=$sampleRate" }
        return this
    }

    fun start(jobKey: JobKey): Gardener {
        activeJobs[jobKey]?.forEach { descriptor ->
            descriptor.bush.start()
            log.info { "JOB[$jobKey] BUSH[${descriptor.bushKey}] Started" }
        }
        return this
    }

    fun job(jobKey: JobKey): List<JobDescriptor> {
        return activeJobs[jobKey] ?: throw IllegalArgumentException("Job with key $jobKey is not found")
    }

    fun stop(jobKey: JobKey) {
        activeJobs.remove(jobKey)?.forEach { descriptor ->
            descriptor.bush.close()
            log.info { "JOB[$jobKey] BUSH[${descriptor.bushKey}] Closed. " }
        }
    }

    fun stopAll() {
        activeJobs.keys.forEach { stop(it) }
    }

    fun getAllFutures(jobKey: JobKey): List<Future<ExecutionResult>> =
            activeJobs[jobKey]
                    ?.map { it.bush.tickPodsFutures() }
                    ?.flatten()
                    ?: emptyList()

    fun jobs(): List<JobKey> = activeJobs.keys().toList()

}
