package io.wavebeans.execution.podproxy

import io.wavebeans.lib.Bean

typealias AnyPodProxy = PodProxy<*>

interface PodProxy<T : Any> : Bean<T> {

    val forPartition: Int

}