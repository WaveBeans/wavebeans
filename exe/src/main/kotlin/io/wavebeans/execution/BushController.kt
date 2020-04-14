package io.wavebeans.execution

import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future

typealias JobKey = Int

class BushController {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val jobs = ConcurrentHashMap<JobKey, MutableList<Bush>>()

    fun addBush(jobKey: JobKey, bushKey: BushKey, pods: List<PodRef>, sampleRate: Float): BushController {
        val bush = Bush(bushKey)
        pods.forEach { bush.addPod(it.instantiate(sampleRate)) }
        jobs.putIfAbsent(jobKey, CopyOnWriteArrayList())
        jobs.getValue(jobKey).add(bush)
        log.info { "JOB[$jobKey] New Bush added with pods=${pods} with sampleRate=$sampleRate" }
        return this
    }

    fun start(jobKey: JobKey): BushController {
        jobs[jobKey]?.forEach { bush ->
            bush.start()
            log.info { "JOB[$jobKey] BUSH[${bush.bushKey}] Started" }
        }
        return this
    }

    fun close(jobKey: JobKey) {
        jobs[jobKey]?.forEach { bush ->
            bush.close()
            log.info { "JOB[$jobKey] BUSH[${bush.bushKey}] Closed. " }
        }
    }

    fun getAllFutures(jobKey: JobKey): List<Future<ExecutionResult>> =
            jobs[jobKey]
                    ?.map { it.tickPodsFutures() }
                    ?.flatten()
                    ?: emptyList()
}
