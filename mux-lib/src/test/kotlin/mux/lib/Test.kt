package mux.lib

import java.lang.Thread.sleep

typealias D = Double

inline fun of(x: Int): D = x.toDouble() / Int.MAX_VALUE
inline fun of(x: D): D = x

fun main() {
    (0..10000909 / 512).asSequence()
            .map { createSampleArray(512) { sampleOf(it) } }
            .toList()
    println("created")
//    val dds = ds.asSequence()
//            .zip(ds.asSequence())
//            .map { it.first * it.second }
//            .toList()
//    println(dds)
    while (true) sleep(1000)
}