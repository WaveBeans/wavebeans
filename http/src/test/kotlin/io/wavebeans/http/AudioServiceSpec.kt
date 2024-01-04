package io.wavebeans.http

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.prop
import io.wavebeans.lib.*
import io.wavebeans.lib.io.WavHeader
import io.wavebeans.lib.io.input
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.TimeseriesTableDriver
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.InputStream

private val sampleRate = 44100.0f

object AudioServiceSpec : Spek({

    describe("Streaming WAV") {
        describe("Sample table") {
            val tableRegistry by memoized(TEST) { mock<TableRegistry>() }
            val tableDriver by memoized(TEST) {
                val driver = mock<TimeseriesTableDriver<Sample>>()
                whenever(driver.tableType).thenReturn(Sample::class)
                whenever(driver.sampleRate).thenReturn(sampleRate)
                driver
            }
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
            it("should start stream limited table") {
                whenever(tableDriver.stream(any())).thenReturn(input8Bit().trim(1000))
                assert8BitLimitedWavOutput<Sample>(service, 1000)
            }
        }

        describe("SampleVector table") {
            val tableRegistry by memoized(TEST) { mock<TableRegistry>() }
            val tableDriver by memoized(TEST) {
                val driver = mock<TimeseriesTableDriver<SampleVector>>()
                whenever(driver.tableType).thenReturn(SampleVector::class)
                whenever(driver.sampleRate).thenReturn(sampleRate)
                driver
            }
            val service by memoized(TEST) {
                whenever(tableRegistry.exists(eq("table"))).thenReturn(true)
                whenever(tableRegistry.byName<SampleVector>("table")).thenReturn(tableDriver)
                AudioService(tableRegistry)
            }

            it("should start streaming 8 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input8Bit().window(16).map { sampleVectorOf(it) })
                assert8BitWavOutput<SampleVector>(service)
            }
            it("should start streaming 16 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input16Bit().window(16).map { sampleVectorOf(it) })
                assert16BitWavOutput<SampleVector>(service)
            }
            it("should start streaming 24 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input24Bit().window(16).map { sampleVectorOf(it) })
                assert24BitWavOutput<SampleVector>(service)
            }
            it("should start streaming 32 bit wav file") {
                whenever(tableDriver.stream(any())).thenReturn(input32Bit().window(16).map { sampleVectorOf(it) })
                assert32BitWavOutput<SampleVector>(service)
            }
            it("should start stream limited table") {
                whenever(tableDriver.stream(any())).thenReturn(input8Bit().trim(1000).window(16).map { sampleVectorOf(it) })
                assert8BitLimitedWavOutput<Sample>(service, 1000)
            }
        }
    }
})

private fun input32Bit() = input { (i, _) -> sampleOf((i and 0xFFFFFFFF).toInt()) }

private fun input24Bit() = input { (i, _) -> sampleOf((i and 0xFFFFFF).toInt(), as24bit = true) }

private fun input16Bit() = input { (i, _) -> sampleOf((i and 0xFFFF).toShort()) }

private fun input8Bit() = input { (i, _) -> sampleOf((i and 0xFF).toByte()) }

private fun <T : Any> assert8BitWavOutput(service: AudioService) {
    assertThat(service.stream<T>(AudioStreamOutputFormat.WAV, "table", BitDepth.BIT_8, null, 0.s)).all {
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

private fun <T : Any> assert24BitWavOutput(service: AudioService) {
    assertThat(service.stream<T>(AudioStreamOutputFormat.WAV, "table", BitDepth.BIT_24, null, 0.s)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(768).isEqualTo(ByteArray(768) { if (it % 3 == 0) (it / 3 and 0xFF).toByte() else if (it % 3 == 1) 0x00 else 0x00 })
        take(768).isEqualTo(ByteArray(768) { if (it % 3 == 0) (it / 3 and 0xFF).toByte() else if (it % 3 == 1) 0x01 else 0x00 })
        take(768).isEqualTo(ByteArray(768) { if (it % 3 == 0) (it / 3 and 0xFF).toByte() else if (it % 3 == 1) 0x02 else 0x00 })
    }
}

private fun <T : Any> assert32BitWavOutput(service: AudioService) {
    assertThat(service.stream<T>(AudioStreamOutputFormat.WAV, "table", BitDepth.BIT_32, null, 0.s)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(1024).isEqualTo(ByteArray(1024) { if (it % 4 == 0) (it / 4 and 0xFF).toByte() else if (it % 4 == 1) 0x00 else 0x00 })
        take(1024).isEqualTo(ByteArray(1024) { if (it % 4 == 0) (it / 4 and 0xFF).toByte() else if (it % 4 == 1) 0x01 else 0x00 })
        take(1024).isEqualTo(ByteArray(1024) { if (it % 4 == 0) (it / 4 and 0xFF).toByte() else if (it % 4 == 1) 0x02 else 0x00 })
    }
}

private fun <T : Any> assert8BitLimitedWavOutput(service: AudioService, expectedLengthMs: Long) {
    assertThat(service.stream<T>(AudioStreamOutputFormat.WAV, "table", BitDepth.BIT_8, null, 0.s)).all {
        val dataSize = (BitDepth.BIT_8.bytesPerSample * sampleRate / 1000.0 * expectedLengthMs).toInt()
        take(44).all {
            isNotEmpty() // header is there
            // size in wav header remains unlimited
            isEqualTo(WavHeader(BitDepth.BIT_8, sampleRate, 1, Int.MAX_VALUE).header())
        }
        // though the data is all there
        take(dataSize).prop("unsignedByte[]") { it.map(Byte::asUnsignedByte).toTypedArray() }
                .isEqualTo(Array(dataSize) { it and 0xFF })
        // and no more left
        tryTake(128).prop("count") { it.first }.isEqualTo(-1)
    }
}

private fun <T : Any> assert16BitWavOutput(service: AudioService) {
    assertThat(service.stream<T>(AudioStreamOutputFormat.WAV, "table", BitDepth.BIT_16, null, 0.s)).all {
        take(44).all {
            isNotEmpty() // header is there
            range(0, 4).isEqualTo("RIFF".toByteArray())
        }
        take(512).isEqualTo(ByteArray(512) { if (it % 2 == 0) (it / 2 and 0xFF).toByte() else 0x00 })
        take(512).isEqualTo(ByteArray(512) { if (it % 2 == 0) (it / 2 and 0xFF).toByte() else 0x01 })
        take(512).isEqualTo(ByteArray(512) { if (it % 2 == 0) (it / 2 and 0xFF).toByte() else 0x02 })
    }
}

private fun Assert<InputStream>.take(count: Int): Assert<ByteArray> = this.prop("take($count)") { it.take(count) }
private fun Assert<InputStream>.tryTake(count: Int): Assert<Pair<Int, ByteArray>> = this.prop("tryTake($count)") { it.tryTake(count) }
private fun Assert<ByteArray>.range(start: Int, end: Int): Assert<ByteArray> = this.prop("range[$start:$end]") { it.copyOfRange(start, end) }

private fun InputStream.take(count: Int): ByteArray {
    val (read, ba) = this.tryTake(count)
    if (read < count) throw IllegalStateException("Expected to read $count bytes but read $read")
    return ba
}

private fun InputStream.tryTake(count: Int): Pair<Int, ByteArray> {
    val ba = ByteArray(count)
    val read = this.read(ba)
    return Pair(read, ba)
}
