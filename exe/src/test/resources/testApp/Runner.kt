package io.wavebeans.test.app

import io.wavebeans.execution.distributed.DistributedOverseer
import io.wavebeans.lib.io.StreamOutput


fun run(outputs: List<StreamOutput<*>>) {
    val distributed = DistributedOverseer(
            outputs,
            /*FACILITATOR_LIST*/listOf()/*FACILITATOR_LIST*/,
            2
    )
    val exceptions = distributed.eval(44100.0f).mapNotNull { it.get().exception }
    println(">>>> EXCEPTIONS")
    exceptions.forEach { e ->
        e.printStackTrace(System.out)
        println("----SPLITTER----")
    }
    println("<<<< EXCEPTIONS")
    distributed.close()
}