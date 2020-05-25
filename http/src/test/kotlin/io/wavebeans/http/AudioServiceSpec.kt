package io.wavebeans.http

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.wavebeans.lib.*
import io.wavebeans.lib.io.input
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.window.window
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.TimeseriesTableDriver
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.*
import org.spekframework.spek2.style.specification.describe
import java.io.InputStream

object AudioServiceSpec : Spek({

    describe("Streaming WAV") {
        describe("Sample table") {
            val tableRegistry by memoized(TEST) { mock<TableRegistry>() }
            val tableDriver by memoized(TEST) { mock<TimeseriesTableDriver<Sample>>() }
            val service by memoized(TEST) {
                whenever(tableRegistry.exists(eq("table"))).thenReturn(true)
                whenever(tableRegistry.byName<Sample>("table")).thenReturn(tableDriver)
                AudioService(tableRegistry)
            }

            it("should start streaming 8 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input8Bit())
                assert8BitWavOutput<Sample>(service)
            }
            it("should start streaming 16 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input16Bit())
                assert16BitWavOutput<Sample>(service)
            }
            it("should start streaming 24 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input24Bit())
                assert24BitWavOutput<Sample>(service)
            }
            it("should start streaming 32 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input32Bit())
                assert32BitWavOutput<Sample>(service)
            }
        }

        describe("SampleArray table") {
            val tableRegistry by memoized(TEST) { mock<TableRegistry>() }
            val tableDriver by memoized(TEST) { mock<TimeseriesTableDriver<SampleArray>>() }
            val service by memoized(TEST) {
                whenever(tableRegistry.exists(eq("table"))).thenReturn(true)
                whenever(tableRegistry.byName<SampleArray>("table")).thenReturn(tableDriver)
                AudioService(tableRegistry)
            }

            it("should start streaming 8 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input8Bit().window(16).map { sampleArrayOf(it) })
                assert8BitWavOutput<SampleArray>(service)
            }
            it("should start streaming 16 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input16Bit().window(16).map { sampleArrayOf(it) })
                assert16BitWavOutput<SampleArray>(service)
            }
            it("should start streaming 24 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input24Bit().window(16).map { sampleArrayOf(it) })
                assert24BitWavOutput<SampleArray>(service)
            }
            it("should start streaming 32 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input32Bit().window(16).map { sampleArrayOf(it) })
                assert32BitWavOutput<SampleArray>(service)
            }
        }
    }
})

private fun input32Bit() = input { (i, _) -> sampleOf((i and 0xFFFFFFFF).toInt()) }

private fun input24Bit() = input { (i, _) -> sampleOf((i and 0xFFFFFF).toInt(), as24bit = true) }

private fun input16Bit() = input { (i, _) -> sampleOf((i and 0xFFFF).toShort()) }

private fun input8Bit() = input { (i, _) -> sampleOf((i and 0xFF).toByte()) }

private inline fun <reified T : Any> assert8BitWavOutput(service: AudioService) {
    assertThat(service.stream(AudioStreamOutputFormat.WAV, "table", 44100.0f, BitDepth.BIT_8, T::class, null)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(256).prop("unsignedByte[]") { it.map(Byte::asUnsignedByte).toTypedArray() }
                .isEqualTo(Array(256) { it and 0xFF })
        take(256).prop("unsignedByte[]") { it.map(Byte::asUnsignedByte).toTypedArray() }
                .isEqualTo(Array(256) { it and 0xFF })
        take(256).prop("unsignedByte[]") { it.map(Byte::asUnsignedByte).toTypedArray() }
                .isEqualTo(Array(256) { it and 0xFF })
    }
}

private inline fun <reified T : Any> assert24BitWavOutput(service: AudioService) {
    assertThat(service.stream(AudioStreamOutputFormat.WAV, "table", 44100.0f, BitDepth.BIT_24, T::class, null)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(768).isEqualTo(ByteArray(768) { if (it % 3 == 0) (it / 3 and 0xFF).toByte() else if (it % 3 == 1) 0x00 else 0x00 })
        take(768).isEqualTo(ByteArray(768) { if (it % 3 == 0) (it / 3 and 0xFF).toByte() else if (it % 3 == 1) 0x01 else 0x00 })
        take(768).isEqualTo(ByteArray(768) { if (it % 3 == 0) (it / 3 and 0xFF).toByte() else if (it % 3 == 1) 0x02 else 0x00 })
    }
}

private inline fun <reified T : Any> assert32BitWavOutput(service: AudioService) {
    assertThat(service.stream(AudioStreamOutputFormat.WAV, "table", 44100.0f, BitDepth.BIT_32, T::class, null)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(1024).isEqualTo(ByteArray(1024) { if (it % 4 == 0) (it / 4 and 0xFF).toByte() else if (it % 4 == 1) 0x00 else 0x00 })
        take(1024).isEqualTo(ByteArray(1024) { if (it % 4 == 0) (it / 4 and 0xFF).toByte() else if (it % 4 == 1) 0x01 else 0x00 })
        take(1024).isEqualTo(ByteArray(1024) { if (it % 4 == 0) (it / 4 and 0xFF).toByte() else if (it % 4 == 1) 0x02 else 0x00 })
    }
}

private inline fun <reified T : Any> assert16BitWavOutput(service: AudioService) {
    assertThat(service.stream(AudioStreamOutputFormat.WAV, "table", 44100.0f, BitDepth.BIT_16, T::class, null)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(512).isEqualTo(ByteArray(512) { if (it % 2 == 0) (it / 2 and 0xFF).toByte() else 0x00 })
        take(512).isEqualTo(ByteArray(512) { if (it % 2 == 0) (it / 2 and 0xFF).toByte() else 0x01 })
        take(512).isEqualTo(ByteArray(512) { if (it % 2 == 0) (it / 2 and 0xFF).toByte() else 0x02 })
    }
}

private fun Assert<InputStream>.take(count: Int): Assert<ByteArray> = this.prop("bytes[$count]") { it.take(count) }
private fun Assert<ByteArray>.range(start: Int, end: Int): Assert<ByteArray> = this.prop("range[$start:$end]") { it.copyOfRange(start, end) }

private fun InputStream.take(count: Int): ByteArray {
    val ba = ByteArray(count)
    val read = this.read(ba)
    if (read < count) throw IllegalStateException("Expected to read $count bytes but read $read")
    return ba
}