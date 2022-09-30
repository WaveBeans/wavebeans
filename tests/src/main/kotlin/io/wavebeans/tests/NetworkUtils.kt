package io.wavebeans.tests

import mu.KotlinLogging
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.nextInt

private val log = KotlinLogging.logger { }

fun createPorts(count: Int, range: IntRange = 2000..65000): Array<Int> = (0 until count).map { findFreePort(range) }.toTypedArray()

/** Make sure the ports are not reused across different port acquiring attempts. */
private val acquiredPorts: MutableMap<Int, Unit> = ConcurrentHashMap()

fun findFreePort(range: IntRange = 2000..65000): Int {
    var port: Int
    var i = 1
    while (true) {
        port = Random.nextInt(range)
        if (port in acquiredPorts.keys) continue
        try {
            val socket = java.net.Socket()
            socket.bind(InetSocketAddress(InetAddress.getByName("localhost"), port))
            socket.close()
            break
        } catch (e: IOException) {
            log.debug(e) { "[Attempt #$i] Port $port is busy" }
            acquiredPorts[port] = Unit
        }
        if (i >= 10) {
            throw IllegalStateException("Can't find free port in range $range within $i attempts")
        }
        i++
    }
    acquiredPorts[port] = Unit
    log.debug { "Acquired port $port, overall list: ${acquiredPorts.keys}" }
    return port
}