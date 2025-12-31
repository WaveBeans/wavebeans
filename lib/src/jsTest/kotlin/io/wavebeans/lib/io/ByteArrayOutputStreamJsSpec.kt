package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ByteArrayOutputStreamJsSpec {
    @Test
    fun writeSingleBytesThenToByteArray() {
        val s = ByteArrayOutputStream()
        s.write(0)
        s.write(0x7F)
        s.write(0x80)
        s.write(0xFF)
        val arr = s.toByteArray()
        assertTrue(arr.contentEquals(byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())))
    }

    @Test
    fun writeArrayWithOffsetAndLength() {
        val s = ByteArrayOutputStream()
        val src = byteArrayOf(1, 2, 3, 4, 5, 6)
        s.write(src, 2, 3)
        assertTrue(s.toByteArray().contentEquals(byteArrayOf(3, 4, 5)))
    }

    @Test
    fun writeManyBytesGrowsBuffer() {
        val s = ByteArrayOutputStream()
        val large = ByteArray(1000) { (it % 256).toByte() }
        s.write(large)
        assertTrue(s.toByteArray().contentEquals(large))
    }

    @Test
    fun zeroLengthWriteIsNoOp() {
        val s = ByteArrayOutputStream()
        val src = byteArrayOf(9, 8, 7)
        s.write(src, 0, 0)
        assertEquals(0, s.toByteArray().size)
    }

    @Test
    fun writingAfterCloseThrows() {
        val s = ByteArrayOutputStream()
        s.close()
        assertFailsWith<IllegalStateException> { s.write(1) }
        assertFailsWith<IllegalStateException> { s.write(byteArrayOf(1)) }
    }

    @Test
    fun invalidBoundsThrow() {
        val s = ByteArrayOutputStream()
        val src = ByteArray(5)
        assertFailsWith<IndexOutOfBoundsException> { s.write(src, -1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { s.write(src, 0, -1) }
        assertFailsWith<IndexOutOfBoundsException> { s.write(src, 3, 5) }
    }
}
