package io.wavebeans.execution.podproxy

import io.wavebeans.lib.Bean

interface PodProxy : Bean<Any> {

    val forPartition: Int

}