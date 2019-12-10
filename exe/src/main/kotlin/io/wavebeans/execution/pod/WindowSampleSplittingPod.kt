package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.WindowSampleArray
import io.wavebeans.execution.medium.createWindowSampleArray
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.WindowStream
import io.wavebeans.lib.stream.window.WindowStreamParams

class WindowSampleSplittingPod(
        bean: WindowStream<Sample>,
        podKey: PodKey,
        partitionCount: Int
) : SplittingPod<Window<Sample>, WindowSampleArray, WindowStream<Sample>>(
        bean = bean,
        podKey = podKey,
        partitionCount = partitionCount,
        converter = { list ->
            val i = list.iterator()
            createWindowSampleArray(list.size) { i.next() }
        }
) {

    @Suppress("unused") // called via reflection
    fun windowStreamParams(): WindowStreamParams = bean.parameters

}