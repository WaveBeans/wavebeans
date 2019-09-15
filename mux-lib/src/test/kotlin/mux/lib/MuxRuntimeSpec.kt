package mux.lib

import mux.lib.execution.MuxTuple
import mux.lib.io.StreamOutput
import mux.lib.io.toCsv
import mux.lib.stream.changeAmplitude
import mux.lib.stream.plus
import mux.lib.stream.sine
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MuxRuntimeSpec : Spek({
    describe("") {
        val outputs = ArrayList<StreamOutput<*, *>>()


        val i1 = 440.sine(0.5)
        val i2 = 800.sine(0.0)

        val p1 = i1.changeAmplitude(1.7)
        val p2 = i2.changeAmplitude(1.8)
                .rangeProjection(0, 1000)

        val o1 = p1
                .trim(5000)
                .toCsv("file:///users/asubb/tmp/o1.csv")
        val o2 = (p1 + p2)
                .trim(3000)
                .toCsv("file:///users/asubb/tmp/o2.csv")


        outputs += o1
        outputs += o2

        fun iterateOverNodes(node: MuxNode<*, *>) {

            val c = node.inputs().size
            println("$node  inputs[$c]=${node.inputs()}")
            node.inputs().forEach { iterateOverNodes(it) }
        }

        outputs.forEach {
            println("OUT $it  input=${it.input()}")
            iterateOverNodes(it.input())
        }

        println("=============")

        fun tuples(o: StreamOutput<*, *>, node: MuxNode<*, *>, prevNodes: List<MuxNode<*, *>> = emptyList()): List<MuxTuple> {
            return if (node.inputs().isEmpty()) {
                listOf(MuxTuple(o, prevNodes))
            } else {
                node.inputs()
                        .map { tuples(o, it, prevNodes + it) }
                        .flatten()
            }
        }

        val tuples = outputs.map { tuples(it, it.input()) }.flatten()

        println(tuples.joinToString("\n\n"))

        tuples.map { Pair(it.base, it.base.writer(44100.0f)) }
                .map { it.second.write(10000); it }
                .map { it.first.close(); it.second.close() }


//        println("========================")
//        val nodes = outputs.map { it.input }
//
//        fun getLongestStrike(muxNode: MuxNode): Pair<List<MuxNode>, MuxNode?> {
//            val strike = ArrayList<MuxNode>()
//            fun iterate(muxNode: MuxNode): MuxNode? {
//                when (muxNode) {
//                    is SingleMuxNode -> {
//                        strike += muxNode
//                        return iterate(muxNode.input)
//                    }
//                    is MultiMuxNode -> {
//                        strike += muxNode
//                        return muxNode
//                    }
//                    is EntryMuxNode -> {
//                        strike += muxNode
//                        return null
//                    }
//                    else -> throw UnsupportedOperationException("${muxNode.javaClass} is not implemented")
//
//                }
//            }
//
//            val terminal = iterate(muxNode)
//            return Pair(strike, terminal)
//        }
//
//        val strikes = HashMap<MuxNode, List<MuxNode>>()
//        nodes.forEach { node ->
//            val iterateOver = Stack<MuxNode>()
//            iterateOver += node
//            while (iterateOver.isNotEmpty()) {
//                val root = iterateOver.pop()
//                val (strike, terminal) = getLongestStrike(root)
//                strikes[root] = strike
//                if (terminal is MultiMuxNode) {
//                    iterateOver += terminal.inputs
//                }
//            }
//        }
//
//        println(
//                strikes.map {
//                    "${it.key} --> [" + it.value.joinToString(",") + "]"
//                }.joinToString("\n\n")
//        )
//
//        println("========================")
//        val newStrikes = HashMap<MuxNode, List<MuxNode>>()
//        strikes.forEach { s1 ->
//            strikes.forEach { s2 ->
//                if (s1 != s2) {
//                    val longer = if (s2.value.size > s1.value.size) s2 else s1
//                    val shorter = if (s2.value.size > s1.value.size) s1 else s2
//                    val maxIterations = longer.value.size - shorter.value.size
//                    val subIndex = (0..maxIterations).firstOrNull { i ->
//                        longer.value.subList(i, i + shorter.value.size) == shorter.value
//                    }
//                    if (subIndex != null) {
//                        val newStrike = longer.value.subList(0, subIndex) +
//                                EntryMuxNode(Mux("RefInput[${shorter.key}]", null))
//                        newStrikes[longer.key] = newStrike
//                    } else {
//                        newStrikes.putIfAbsent(longer.key, longer.value)
//                        newStrikes.putIfAbsent(shorter.key, shorter.value)
//                    }
//                }
//            }
//        }
//
//        println(
//                newStrikes.map {
//                    "${it.key} --> [" + it.value.joinToString(",") + "]"
//                }.joinToString("\n\n")
//        )
//
//        println("========================")
//
//        // execute new topology
//        val writers = newStrikes
//                .filter { it.key.mux.stream is StreamOutput }
//                .map { (it.key as StreamOutput).writer(44100.0f) }
//
//        var allRead = false
//        while (!allRead) {
//            allRead = writers.none { it.write(10) }
//            Thread.sleep(1)
//        }
//
//        writers.forEach { it.close() }
//        outputs.forEach { it.close() }
    }
})