package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ByteArrayInputStreamJsSpec {
    @Test
    fun readSingleBytesThenEof() {
        val src = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())
        val s = ByteArrayInputStream(src)
        assertEquals(0, s.read())
        assertEquals(0x7F, s.read())
        assertEquals(0x80, s.read())
        assertEquals(0xFF, s.read())
        assertEquals(-1, s.read())
        assertEquals(-1, s.read())
    }

    @Test
    fun bulkReadFillsTargetThenEof() {
        val src = (0 until 10).map { it.toByte() }.toByteArray()
        val s = ByteArrayInputStream(src)
        val buf = ByteArray(16)
        val n1 = s.read(buf)
        assertEquals(10, n1)
        assertTrue(buf.copyOfRange(0, 10).contentEquals(src))
        val n2 = s.read(buf)
        assertEquals(-1, n2)
    }

    @Test
    fun bulkReadWithOffsetAndLength() {
        val src = byteArrayOf(1,2,3,4,5)
        val s = ByteArrayInputStream(src)
        val buf = ByteArray(10) { (-1).toByte() }
        val n = s.read(buf, 2, 3)
        assertEquals(3, n)
        assertTrue(buf.contentEquals(byteArrayOf(-1, -1, 1, 2, 3, -1, -1, -1, -1, -1)))
    }

    @Test
    fun zeroLengthReadReturnsZeroAndDoesNotAdvance() {
        val src = byteArrayOf(1,2,3)
        val s = ByteArrayInputStream(src)
        val buf = ByteArray(5)
        val n = s.read(buf, 0, 0)
        assertEquals(0, n)
        assertEquals(1, s.read())
    }

    @Test
    fun invalidBoundsThrow() {
        val s = ByteArrayInputStream(byteArrayOf(1,2,3))
        val buf = ByteArray(4)
        assertFailsWith<IndexOutOfBoundsException> { s.read(buf, -1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { s.read(buf, 0, -1) }
        assertFailsWith<IndexOutOfBoundsException> { s.read(buf, 2, 3) }
    }

    @Test
    fun readingAfterCloseThrows() {
        val s = ByteArrayInputStream(byteArrayOf(1))
        s.close()
        assertFailsWith<IllegalStateException> { s.read() }
        assertFailsWith<IllegalStateException> { s.read(ByteArray(1)) }
    }
}
