package io.wavebeans.tests

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger { }

fun createPorts(count: Int): Array<Int> = (0 until count).map { findFreePort() }.toTypedArray()

/** Make sure the ports are not reused across different port acquiring attempts. */
private val acquiredPorts: MutableMap<Int, Unit> = ConcurrentHashMap()

fun findFreePort(): Int {
    var port: Int
    while (true) {
        val socket = ServerSocket(0)
        port = socket.localPort
        socket.close()
        if (port in acquiredPorts.keys) continue
        acquiredPorts[port] = Unit
        break
    }
    log.debug { "Acquired port $port, overall list: ${acquiredPorts.keys}" }
    return port
}