package io.wavebeans.execution

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.pod.SplittingPod
import io.wavebeans.execution.pod.StreamingPod
import io.wavebeans.execution.podproxy.*
import io.wavebeans.lib.*
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.stream.div
import io.wavebeans.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KTypeProjection.Companion.covariant
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

class PodRefSpec : Spek({

    describe("One bean per pod") {

        describe("Single input bean (Single or Alter)") {

            describe("Single Link to another pod. I.e. non-partitioned infinite bean.") {
                val bean = 440.sine().div(2.0) // sine(0) <- div(1)
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
                    assertThat(podRef.instantiate(1.0f))
                            .isInstanceOf(StreamingPod::class).all {
                                proxies()
                                        .eachIndexed(1) { proxy, _ ->
                                            proxy.isInstanceOf(StreamingPodProxy::class).all {
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
                    // sine(0) <- div(1.0, 1.1) <- trim(2)
                    val bean = 440.sine().div(2.0).trim(1)
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
                        assertThat(podRef.instantiate(1.0f))
                                .isInstanceOf(SplittingPod::class).all {
                                    proxies()
                                            .eachIndexed(1) { proxy, _ ->
                                                proxy.isInstanceOf(MergingPodProxy::class).all {
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
                    assertThat(podRef.instantiate(1.0f))
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
                    assertThat(podRef.instantiate(1.0f))
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
                    assertThat(podRef.instantiate(1.0f))
                            .isInstanceOf(SplittingPod::class).all {
                                proxies()
                                        .eachIndexed(2) { proxy, i ->
                                            when (i) {
                                                0 -> proxy.isInstanceOf(StreamingPodProxy::class).all {
                                                    pointedTo().isEqualTo(PodKey(1, 0))
                                                    forPartition().isEqualTo(0)
                                                }
                                                1 -> proxy.isInstanceOf(StreamingPodProxy::class).all {
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
                    assertThat(podRef.instantiate(1.0f))
                            .isInstanceOf(SplittingPod::class).all {
                                proxies()
                                        .eachIndexed(2) { proxy, i ->
                                            when (i) {
                                                0 -> proxy.isInstanceOf(MergingPodProxy::class).all {
                                                    readsFrom().isListOf(PodKey(1, 0), PodKey(1, 1))
                                                    forPartition().isEqualTo(0)
                                                }
                                                1 -> proxy.isInstanceOf(MergingPodProxy::class).all {
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

private fun input1Type(bean: AnyBean) = bean.inputs().first()::class.let {
    val parameters = it.typeParameters.map { STAR }
    it.createType(parameters)
}

private fun input2Type(bean: AnyBean) = bean.inputs().drop(1).first()::class.createType()

private fun Assert<Pod>.proxies() = prop("proxies") {
    when (it) {
        is SplittingPod<*, *> -> it.bean.inputs()
        is StreamingPod<*, *> -> it.bean.inputs()
        else -> throw UnsupportedOperationException()
    }
}

private fun Assert<Pod>.podKey() = prop("podKey") { it.podKey }

private fun Assert<StreamingPodProxy>.pointedTo() = prop("pointedTo") { it.pointedTo }
private fun Assert<MergingPodProxy>.readsFrom() = prop("pointedTo") { it.readsFrom }
private fun Assert<PodProxy>.forPartition() = prop("forPartition") { it.forPartition }

internal class TestPartitionableStreamingInput(
        override val parameters: BeanParams
) : BeanStream<Sample>, SourceBean<Sample> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> = throw UnsupportedOperationException()

    override val desiredSampleRate: Float?
        get() = throw UnsupportedOperationException()
}

internal class TestSinglePartitionStreamingInput(
        override val parameters: BeanParams
) : BeanStream<Sample>, SourceBean<Sample>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<Sample> = throw UnsupportedOperationException()

    override val desiredSampleRate: Float?
        get() = throw UnsupportedOperationException()
}

internal class TestMultiBean(
        val input1: BeanStream<Sample>,
        val input2: BeanStream<Sample>,
        override val parameters: BeanParams
) : BeanStream<Sample>, MultiBean<Sample> {

    override val inputs: List<Bean<Sample>>
        get() = listOf(input1, input2)

    override fun asSequence(sampleRate: Float): Sequence<Sample> = throw UnsupportedOperationException()

    override val desiredSampleRate: Float?
        get() = throw UnsupportedOperationException()
}
