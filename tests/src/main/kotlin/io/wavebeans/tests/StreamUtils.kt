package io.wavebeans.tests

import io.wavebeans.execution.MultiThreadedOverseer
import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Fn
import io.wavebeans.lib.io.*
import io.wavebeans.lib.sampleOf
import java.lang.Thread.sleep

/**
 * Generates sequential stream of (index * 1e-10)
 */
fun seqStream() = input { sampleOf(it.first * 1e-10) }

class StoreToMemoryFn<T : Any> : Fn<WriteFunctionArgument<T>, Boolean>() {

    private val list = ArrayList<T>()

    override fun apply(argument: WriteFunctionArgument<T>): Boolean {
        if (argument.phase == WriteFunctionPhase.WRITE)
            list += argument.sample!!
        return true
    }

    fun list(): List<T> = list
}


inline fun <reified T : Any> BeanStream<T>.toList(sampleRate: Float, take: Int = Int.MAX_VALUE, drop: Int = 0): List<T> {
    val writeFunction = StoreToMemoryFn<T>()
    this.out(writeFunction).evaluate(sampleRate)
    return writeFunction.list().drop(drop).take(take)
}

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