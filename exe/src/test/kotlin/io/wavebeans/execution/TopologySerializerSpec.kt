package io.wavebeans.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.TrimmedFiniteSampleStream
import io.wavebeans.lib.stream.TrimmedFiniteSampleStreamParams
import io.wavebeans.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopologySerializerSpec : Spek({

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
            println(topologySerialized)
            deserialize(topologySerialized)
        }

        it("has same refs") {
            assertThat(deserializedTopology.refs).all {
                size().isEqualTo(topology.refs.size)
                each { nodeRef ->

                    nodeRef.prop("type") { it.type }.isIn(*listOf(
                            SineGeneratedInput::class,
                            CsvSampleStreamOutput::class,
                            TrimmedFiniteSampleStream::class
                    ).map { it.qualifiedName }.toTypedArray())

                    nodeRef.prop("params") { it.params }.kClass().isIn(*listOf(
                            SineGeneratedInputParams::class,
                            CsvSampleStreamOutputParams::class,
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

})