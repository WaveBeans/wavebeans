package io.wavebeans.tests

import io.wavebeans.execution.MultiThreadedOverseer
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.lib.io.StreamOutput
import java.lang.Thread.sleep

fun <T : Any> StreamOutput<T>.evaluate(sampleRate: Float) {
    this.writer(sampleRate).use {
        while (it.write()) {
            sleep(0)
        }
    }
}

fun <T : Any> StreamOutput<T>.evaluateInDistributedMode(
        sampleRate: Float,
        facilitatorLocations: List<String>,
        partitionsCount: Int = 2
) {
    DistributedOverseer(
            outputs = listOf(this),
            facilitatorLocations = facilitatorLocations,
            httpLocations = emptyList(),
            partitionsCount = partitionsCount
    ).use {
        it.eval(sampleRate).all { f -> f.get().finished }
    }
}

fun <T : Any> StreamOutput<T>.evaluateInMultiThreadedMode(
        sampleRate: Float,
        partitionsCount: Int = 2,
        threadsCount: Int = 2
) {
    MultiThreadedOverseer(
            outputs = listOf(this),
            partitionsCount = partitionsCount,
            threadsCount = threadsCount
    ).use {
        it.eval(sampleRate).all { f -> f.get().finished }
    }
}