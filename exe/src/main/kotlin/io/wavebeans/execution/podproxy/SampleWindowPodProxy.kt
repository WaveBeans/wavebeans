package io.wavebeans.execution.podproxy

import io.wavebeans.execution.medium.*
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.window.SampleWindowStream
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.WindowStreamParams
import java.util.concurrent.TimeUnit

class SampleWindowStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : SampleWindowStream, StreamingPodProxy<Window<Sample>, SampleWindowStream, WindowSampleArray>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableWindowSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) Window(arr[i].toList()) else null }
) {
    override val parameters: WindowStreamParams
        get() {
            val pod = pointedTo
            val bush = podDiscovery.bushFor(pod)
            val caller = bushCallerRepository.create(bush, pod)
            return caller.call("windowStreamParams").get(5000, TimeUnit.MILLISECONDS).windowStreamParams()
        }
}

class SampleWindowMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Window<Sample>, SampleWindowStream, WindowSampleArray>(
        forPartition = forPartition,
        converter = { it.nullableWindowSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) Window(arr[i].toList()) else null }
), SampleWindowStream {
    override val parameters: WindowStreamParams
        get() {
            val pod = readsFrom.first()
            val bush = podDiscovery.bushFor(pod)
            val caller = bushCallerRepository.create(bush, pod)
            return caller.call("windowStreamParams").get(5000, TimeUnit.MILLISECONDS).windowStreamParams()
        }
}