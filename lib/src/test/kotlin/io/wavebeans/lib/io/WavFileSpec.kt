package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isCloseTo
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object WavFileSpec : Spek({
    val sampleRate = 192000.0f
    describe("Mono") {

        val input = 440.sine().trim(2)
        val expectedSize = 384

        describe("8 bit") {
            val file = File.createTempFile("wavtest", ".wav")
            val filePath = file.absolutePath
            val o = input.toMono8bitWav("file://$filePath")

            beforeGroup {
                o.writer(sampleRate).use {
                    while (it.write()) {
                    }
                }
            }

            it("should read the same sine") {
                val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                assertThat(wave("file://$filePath").asSequence(sampleRate).toList())
                        .eachIndexed(expectedSize) { sample, idx ->
                            sample.isCloseTo(inputAsArray[idx], 1e-2)
                        }
            }
        }
        describe("16 bit") {
            val file = File.createTempFile("wavtest", ".wav")
            val filePath = file.absolutePath
            val o = input.toMono16bitWav("file://$filePath")

            beforeGroup {
                o.writer(sampleRate).use {
                    while (it.write()) {
                    }
                }
            }

            it("should read the same sine") {
                val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                assertThat(wave("file://$filePath").asSequence(sampleRate).toList())
                        .eachIndexed(expectedSize) { sample, idx ->
                            sample.isCloseTo(inputAsArray[idx], 1e-4)
                        }
            }
        }
        describe("24 bit") {
            val file = File.createTempFile("wavtest", ".wav")
            val filePath = file.absolutePath
            val o = input.toMono24bitWav("file://$filePath")

            beforeGroup {
                o.writer(sampleRate).use {
                    while (it.write()) {
                    }
                }
            }

            it("should read the same sine") {
                val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                assertThat(wave("file://$filePath").asSequence(sampleRate).toList())
                        .eachIndexed(expectedSize) { sample, idx ->
                            sample.isCloseTo(inputAsArray[idx], 1e-6)
                        }
            }
        }
        describe("32 bit") {
            val file = File.createTempFile("wavtest", ".wav")
            val filePath = file.absolutePath
            val o = input.toMono32bitWav("file://$filePath")

            beforeGroup {
                o.writer(sampleRate).use {
                    while (it.write()) {
                    }
                }
            }

            it("should read the same sine") {
                val inputAsArray = input.asSequence(sampleRate).toList().toTypedArray()
                assertThat(wave("file://$filePath").asSequence(sampleRate).toList())
                        .eachIndexed(expectedSize) { sample, idx ->
                            sample.isCloseTo(inputAsArray[idx], 1e-9)
                        }
            }
        }
    }
})