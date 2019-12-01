package io.wavebeans.execution.podproxy

import io.wavebeans.lib.Bean

typealias AnyPodProxy = PodProxy<*, *>

interface PodProxy<T : Any, S : Any> : Bean<T, S> {

    val forPartition: Int

}