package mux.lib.execution

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import mux.lib.*
import mux.lib.io.StreamInput
import mux.lib.io.sine
import mux.lib.stream.SampleStream
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.createType

@ExperimentalStdlibApi
class PodRefSpec : Spek({

    describe("One bean per pod") {

        describe("Single input bean (Single or Alter)") {

            describe("Single Link to another pod. I.e. non-partitioned infinite bean.") {
                val bean = 440.sine() // sine(0) <- inf(1)
                val podProxy = PodProxyRef(
                        input1Type(bean),
                        listOf(PodKey(0, 0)),
                        0
                )
                val podRef = PodRef(
                        PodKey(1, 0),
                        listOf(BeanRef.create(1, bean::class, bean.parameters)),
                        emptyList(),
                        listOf(podProxy)
                )

                it("should create pod") {
                    assertThat(podRef.instantiate())
                            .isInstanceOf(StreamingPod::class).all {
                                proxies()
                                        .eachIndexed(1) { proxy, i ->
                                            proxy.isInstanceOf(StreamInputPodProxy::class).all {
                                                pointedTo().isEqualTo(PodKey(0, 0))
                                                forPartition().isEqualTo(0)
                                            }
                                        }
                                podKey().isEqualTo(PodKey(1, 0))
                            }
                }
            }

            describe("Multiple Links to another pods. Merging behavior.") {

                describe("non-single partition bean. I.e. partitioned trim bean") {
                    // sine(0) <- inf(1.0, 1.1) <- trim(2)
                    val bean = 440.sine().trim(1)
                    val podProxy = PodProxyRef(
                            input1Type(bean),
                            listOf(PodKey(1, 0), PodKey(1, 1)),
                            0
                    )
                    val podRef = PodRef(
                            PodKey(2, 0),
                            listOf(BeanRef.create(2, bean::class, bean.parameters)),
                            emptyList(),
                            listOf(podProxy),
                            2
                    )

                    it("should create pod") {
                        assertThat(podRef.instantiate())
                                .isInstanceOf(SplittingPod::class).all {
                                    proxies()
                                            .eachIndexed(1) { proxy, i ->
                                                proxy.isInstanceOf(SampleStreamMergingPodProxy::class).all {
                                                    readsFrom().isListOf(PodKey(1, 0), PodKey(1, 1))
                                                    forPartition().isEqualTo(0)
                                                }
                                            }
                                    podKey().isEqualTo(PodKey(2, 0))
                                }
                    }
                }
            }
        }

        describe("Source bean") {

            describe("Single partition bean") {
                val bean = TestSinglePartitionStreamingInput(NoParams())

                val podRef = PodRef(
                        PodKey(0, 0),
                        listOf(BeanRef.create(1, bean::class, bean.parameters)),
                        emptyList(),
                        listOf()
                )

                it("should create pod") {
                    assertThat(podRef.instantiate())
                            .isInstanceOf(StreamingPod::class).all {
                                proxies().isEmpty()
                                podKey().isEqualTo(PodKey(0, 0))
                            }
                }
            }

            describe("Partitionable input") {
                val bean = TestPartitionableStreamingInput(NoParams())

                val podRef = PodRef(
                        PodKey(0, 0),
                        listOf(BeanRef.create(1, bean::class, bean.parameters)),
                        emptyList(),
                        listOf(),
                        2
                )

                it("should create pod") {
                    assertThat(podRef.instantiate())
                            .isInstanceOf(SplittingPod::class).all {
                                proxies().isEmpty()
                                podKey().isEqualTo(PodKey(0, 0))
                            }
                }
            }
        }

        describe("Multi bean") {

            val bean = TestMultiBean(
                    440.sine(),
                    880.sine(),
                    NoParams()
            )

            describe("Non partitioned") {
                val podProxy1 = PodProxyRef(
                        input1Type(bean),
                        listOf(PodKey(1, 0)),
                        0
                )
                val podProxy2 = PodProxyRef(
                        input2Type(bean),
                        listOf(PodKey(2, 0)),
                        0
                )
                val podRef = PodRef(
                        PodKey(3, 0),
                        listOf(BeanRef.create(3, bean::class, bean.parameters)),
                        emptyList(),
                        listOf(podProxy1, podProxy2),
                        0
                )

                it("should create pod") {
                    assertThat(podRef.instantiate())
                            .isInstanceOf(SplittingPod::class).all {
                                proxies()
                                        .eachIndexed(2) { proxy, i ->
                                            when (i) {
                                                0 -> proxy.isInstanceOf(SampleStreamPodProxy::class).all {
                                                    pointedTo().isEqualTo(PodKey(1, 0))
                                                    forPartition().isEqualTo(0)
                                                }
                                                1 -> proxy.isInstanceOf(SampleStreamPodProxy::class).all {
                                                    pointedTo().isEqualTo(PodKey(2, 0))
                                                    forPartition().isEqualTo(0)
                                                }
                                            }
                                        }
                                podKey().isEqualTo(PodKey(3, 0))
                            }
                }

            }
            describe("Partitioned") {

                val podProxy1 = PodProxyRef(
                        input1Type(bean),
                        listOf(PodKey(1, 0), PodKey(1, 1)),
                        0
                )
                val podProxy2 = PodProxyRef(
                        input2Type(bean),
                        listOf(PodKey(2, 0), PodKey(2, 1)),
                        0
                )
                val podRef = PodRef(
                        PodKey(3, 0),
                        listOf(BeanRef.create(3, bean::class, bean.parameters)),
                        emptyList(),
                        listOf(podProxy1, podProxy2),
                        0
                )

                it("should create pod") {
                    assertThat(podRef.instantiate())
                            .isInstanceOf(SplittingPod::class).all {
                                proxies()
                                        .eachIndexed(2) { proxy, i ->
                                            when (i) {
                                                0 -> proxy.isInstanceOf(SampleStreamMergingPodProxy::class).all {
                                                    readsFrom().isListOf(PodKey(1, 0), PodKey(1, 1))
                                                    forPartition().isEqualTo(0)
                                                }
                                                1 -> proxy.isInstanceOf(SampleStreamMergingPodProxy::class).all {
                                                    readsFrom().isListOf(PodKey(2, 0), PodKey(2, 1))
                                                    forPartition().isEqualTo(0)
                                                }
                                            }
                                        }
                                podKey().isEqualTo(PodKey(3, 0))
                            }
                }

            }
        }
    }
})

private fun input1Type(bean: AnyBean) = bean.inputs().first()::class.createType()
private fun input2Type(bean: AnyBean) = bean.inputs().drop(1).first()::class.createType()

private fun Assert<Pod>.proxies() = prop("proxies") {
    when (it) {
        is SplittingPod -> it.bean.inputs()
        is StreamingPod -> it.bean.inputs()
        else -> throw UnsupportedOperationException()
    }
}

private fun Assert<Pod>.podKey() = prop("podKey") { it.podKey }

private fun Assert<StreamingPodProxy<*, *>>.pointedTo() = prop("pointedTo") { it.pointedTo }
private fun Assert<MergingPodProxy<*, *>>.readsFrom() = prop("pointedTo") { it.readsFrom }
private fun Assert<PodProxy<*, *>>.forPartition() = prop("forPartition") { it.forPartition }

internal class TestPartitionableStreamingInput(
        override val parameters: BeanParams
) : StreamInput {
    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = throw UnsupportedOperationException()

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput = throw UnsupportedOperationException()
}

internal class TestSinglePartitionStreamingInput(
        override val parameters: BeanParams
) : StreamInput, SinglePartitionBean {
    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = throw UnsupportedOperationException()

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput = throw UnsupportedOperationException()
}

internal class TestMultiBean(
        val input1: BeanStream<SampleArray, SampleStream>,
        val input2: BeanStream<SampleArray, SampleStream>,
        override val parameters: BeanParams
) : SampleStream, MultiBean<SampleArray, SampleStream> {

    override val inputs: List<Bean<SampleArray, SampleStream>>
        get() = listOf(input1, input2)

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = throw UnsupportedOperationException()

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream = throw UnsupportedOperationException()
}
