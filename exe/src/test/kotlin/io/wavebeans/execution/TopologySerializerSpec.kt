package io.wavebeans.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.window.WindowStream
import io.wavebeans.lib.stream.window.WindowStreamParams
import io.wavebeans.lib.stream.window.plus
import io.wavebeans.lib.stream.window.window
import io.wavebeans.lib.table.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopologySerializerSpec : Spek({

    val log = KotlinLogging.logger {}

    describe("2 In 2 Out topology") {

        val i1 = 440.sine(0.5)
        val i2 = 800.sine(0.2)

        val o1 = i1
                .trim(5000)
                .toCsv("file:///some1.csv")
        val o2 = i2
                .trim(3000)
                .toCsv("file:///some2.csv")

        val topology = listOf(o1, o2).buildTopology()

        val deserializedTopology = with(TopologySerializer) {
            val topologySerialized = serialize(topology)
            deserialize(topologySerialized)
        }

        it("has same refs") {
            assertThat(deserializedTopology.refs).all {
                size().isEqualTo(topology.refs.size)
                each { nodeRef ->

                    nodeRef.prop("type") { it.type }.isIn(*listOf(
                            SineGeneratedInput::class,
                            CsvStreamOutput::class,
                            TrimmedFiniteStream::class
                    ).map { it.qualifiedName }.toTypedArray())

                    nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                            SineGeneratedInputParams::class,
                            CsvStreamOutputParams::class,
                            TrimmedFiniteSampleStreamParams::class,
                            NoParams::class
                    ).toTypedArray())
                }
            }
        }
        it("has same links") {
            assertThat(deserializedTopology.links).all {
                size().isEqualTo(topology.links.size)
                each {
                    it.isIn(*topology.links.toTypedArray())
                }
            }
        }
    }

    describe("Input function") {

        val i1 = input { (x, _) -> sampleOf(x) }

        class InputFn(initParameters: FnInitParameters) : Fn<Pair<Long, Float>, Sample?>(initParameters) {
            override fun apply(argument: Pair<Long, Float>): Sample? {
                return sampleOf(argument.first) * initParams.int("factor")
            }
        }

        val inputParameter = 2
        val i2 = input(InputFn(FnInitParameters().add("factor", inputParameter)))

        val o1 = i1
                .trim(5000)
                .toCsv("file:///some1.csv")
        val o2 = i2
                .trim(3000)
                .toCsv("file:///some2.csv")

        val topology = listOf(o1, o2).buildTopology()

        val deserializedTopology = with(TopologySerializer) {
            val topologySerialized = serialize(topology)
            deserialize(topologySerialized)
        }

        it("has same refs") {
            assertThat(deserializedTopology.refs).all {
                size().isEqualTo(topology.refs.size)
                each { nodeRef ->

                    nodeRef.prop("type") { it.type }.isIn(*listOf(
                            Input::class,
                            CsvStreamOutput::class,
                            TrimmedFiniteStream::class
                    ).map { it.qualifiedName }.toTypedArray())

                    nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                            InputParams::class,
                            CsvStreamOutputParams::class,
                            TrimmedFiniteSampleStreamParams::class,
                            NoParams::class
                    ).toTypedArray())
                }
            }
        }
        it("has same links") {
            assertThat(deserializedTopology.links).all {
                size().isEqualTo(topology.links.size)
                each {
                    it.isIn(*topology.links.toTypedArray())
                }
            }
        }
    }

    describe("Map function") {
        class MapFn(initParams: FnInitParameters) : Fn<Sample, Sample>(initParams) {
            override fun apply(argument: Sample): Sample {
                val f = initParams["factor"]?.toInt()!!
                return argument * f
            }
        }

        val factor = 2 // checking passing parameters from closure
        val o = listOf(
                seqStream()
                        .map { it * 2 }
                        .toDevNull(),
                seqStream()
                        .map(MapFn(FnInitParameters().add("factor", factor.toString())))
                        .toDevNull()
        )

        val topology = o.buildTopology()

        val deserializedTopology = with(TopologySerializer) {
            val topologySerialized = serialize(topology)
            deserialize(topologySerialized)
        }

        it("has same refs") {
            assertThat(deserializedTopology.refs).all {
                size().isEqualTo(topology.refs.size)
                each { nodeRef ->

                    nodeRef.prop("type") { it.type }.isIn(*listOf(
                            SeqInput::class,
                            DevNullStreamOutput::class,
                            MapStream::class
                    ).map { it.qualifiedName }.toTypedArray())

                    nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                            SineGeneratedInputParams::class,
                            NoParams::class,
                            MapStreamParams::class
                    ).toTypedArray())
                }
            }
        }
        it("has same links") {
            assertThat(deserializedTopology.links).all {
                size().isEqualTo(topology.links.size)
                each {
                    it.isIn(*topology.links.toTypedArray())
                }
            }
        }
    }

    describe("Merge function") {

        val factor = 2 + 2 * 2

        class MergeFn(initParams: FnInitParameters) : Fn<Pair<Sample?, Sample?>, Sample?>(initParams) {
            override fun apply(argument: Pair<Sample?, Sample?>): Sample? {
                val f = initParams["factor"]?.toInt()!!
                return argument.first ?: ZeroSample * f + argument.second
            }
        }

        val functions = mapOf(
                "Sample merge with Lambda" to listOf(
                        seqStream()
                                .merge(
                                        with = seqStream()
                                ) { (x, y) -> x ?: ZeroSample + y }
                                .toDevNull()
                ),
                "Sample merge with Fn and using outside data" to listOf(
                        seqStream()
                                .merge(
                                        with = seqStream(),
                                        merge = MergeFn(FnInitParameters().add("factor", factor.toString()))
                                )
                                .toDevNull()
                ),
                "Window<Sample>.plus()" to listOf(
                        seqStream().window(2)
                                .plus(seqStream().window(2))
                                .toDevNull()
                ),
                "Sample.plus()" to listOf(
                        seqStream()
                                .plus(seqStream())
                                .toDevNull()
                )
        )

        functions.forEach { (desc, o) ->
            describe(desc) {
                val topology = o.buildTopology()

                val deserializedTopology = with(TopologySerializer) {
                    val topologySerialized = serialize(topology).also { log.debug { it } }
                    deserialize(topologySerialized)
                }

                it("has same refs") {
                    assertThat(deserializedTopology.refs).all {
                        size().isEqualTo(topology.refs.size)
                        each { nodeRef ->

                            nodeRef.prop("type") { it.type }.isIn(*listOf(
                                    SeqInput::class,
                                    WindowStream::class,
                                    DevNullStreamOutput::class,
                                    FunctionMergedStream::class
                            ).map { it.qualifiedName }.toTypedArray())

                            nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                                    SineGeneratedInputParams::class,
                                    WindowStreamParams::class,
                                    NoParams::class,
                                    FunctionMergedStreamParams::class
                            ).toTypedArray())
                        }
                    }
                }
                it("has same links") {
                    assertThat(deserializedTopology.links).all {
                        size().isEqualTo(topology.links.size)
                        each {
                            it.isIn(*topology.links.toTypedArray())
                        }
                    }
                }
            }
        }
    }

    describe("List as input") {

        @Serializable
        data class A(val v: String, val f: Float)

        val o = listOf(1, 2, 3, 4).input().toDevNull()
        val o2 = listOf(A("1", 1.0f), A("2", 2.0f), A("3", 3.0f), A("4", 4.0f)).input().toDevNull()
        val topology = listOf(o, o2).buildTopology()

        val deserializedTopology = with(TopologySerializer) {
            val topologySerialized = serialize(topology).also { log.debug { it } }
            deserialize(topologySerialized)
        }.also { log.debug { it } }

        it("has same refs") {
            assertThat(deserializedTopology.refs).all {
                size().isEqualTo(topology.refs.size)
                each { nodeRef ->

                    nodeRef.prop("type") { it.type }.isIn(*listOf(
                            ListAsInput::class,
                            DevNullStreamOutput::class
                    ).map { it.qualifiedName }.toTypedArray())

                    nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                            ListAsInputParams::class,
                            NoParams::class
                    ).toTypedArray())
                }
            }
        }
        it("has same links") {
            assertThat(deserializedTopology.links).all {
                size().isEqualTo(topology.links.size)
                each {
                    it.isIn(*topology.links.toTypedArray())
                }
            }
        }

    }

    describe("Table sink") {
        val o = seqStream().toTable("table1")
        val q = TableRegistry.default.byName<Sample>("table1").last(2000.ms).toCsv("file:///path/to.csv")

        val topology = listOf(o, q).buildTopology()
        val deserializedTopology = with(TopologySerializer) {
            val topologySerialized = serialize(topology).also { log.debug { it } }
            deserialize(topologySerialized)
        }

        it("has same refs") {
            assertThat(deserializedTopology.refs).all {
                size().isEqualTo(topology.refs.size)
                each { nodeRef ->

                    nodeRef.prop("type") { it.type }.isIn(*listOf(
                            SeqInput::class,
                            TableOutput::class,
                            TableDriverInput::class,
                            CsvStreamOutput::class
                    ).map { it.qualifiedName }.toTypedArray())

                    nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                            NoParams::class,
                            TableOutputParams::class,
                            TableDriverStreamParams::class,
                            CsvStreamOutputParams::class
                    ).toTypedArray())
                }
            }
        }

        it("has same links") {
            assertThat(deserializedTopology.links).all {
                size().isEqualTo(topology.links.size)
                each {
                    it.isIn(*topology.links.toTypedArray())
                }
            }
        }
    }
})