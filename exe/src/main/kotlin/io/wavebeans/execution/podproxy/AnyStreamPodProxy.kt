package io.wavebeans.execution.podproxy

import io.wavebeans.execution.medium.MediumConverter
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.execution.pod.TransferContainer
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

class AnyStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : BeanStream<Any>, StreamingPodProxy<Any, TransferContainer>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = MediumConverter::convert,
        elementExtractor = MediumConverter::extractElement
)

class AnyStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Any, TransferContainer>(
        forPartition = forPartition,
        converter = MediumConverter::convert,
        elementExtractor = MediumConverter::extractElement
), BeanStream<Any>
