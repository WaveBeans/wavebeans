//package mux.lib.execution
//
//import mux.lib.io.*
//import java.lang.Thread.sleep
//import java.util.concurrent.TimeUnit
//
//class MuxRuntime(val sampleRate: Float) {
//
//    private val outputs = ArrayList<StreamOutput>()
//
//    fun addOutput(output: StreamOutput): MuxRuntime {
//        outputs += output
//        return this
//    }
//
//    fun execute() {
////        val nodes = outputs.map { it.mux() }
////        println(nodes.joinToString("\n\n"))
//
//        // just mock implementation
//        val step = 10L
//        val tu = TimeUnit.MILLISECONDS
//
//        val writers = outputs.map { it.writer(sampleRate) }
//
//        var allRead = false
//        while (!allRead) {
//            allRead = writers.none { it.write(step, tu) }
//            sleep(1)
//        }
//
//        writers.forEach { it.close() }
//        outputs.forEach { it.close() }
//    }
//}