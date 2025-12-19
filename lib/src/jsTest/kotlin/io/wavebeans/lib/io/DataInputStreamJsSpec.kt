package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DataInputStreamJsSpec {
    @Test
    fun delegatesSingleByteAndBulkReadsAndHonorsEof() {
        val src = byteArrayOf(1, 2, 3)
        val dis = DataInputStream(ByteArrayInputStream(src))
        assertEquals(1, dis.read())
        val buf = ByteArray(4)
        val n = dis.read(buf)
        assertEquals(2, n)
        assertTrue(buf.copyOfRange(0, 2).contentEquals(byteArrayOf(2, 3)))
        assertEquals(-1, dis.read())
    }

    @Test
    fun readIntReadsBigEndian4BytesAndReturnsMinus1OnEof() {
        val src = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val dis = DataInputStream(ByteArrayInputStream(src))
        assertEquals(0x01020304, dis.readInt())
        assertEquals(-1, dis.readInt())
    }

    @Test
    fun readShortReadsBigEndian2BytesAndReturnsMinus1ShortOnEof() {
        val src = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
        val dis = DataInputStream(ByteArrayInputStream(src))
        assertEquals(0xABCD.toShort().toInt(), dis.readShort().toInt())
        assertEquals((-1).toShort().toInt(), dis.readShort().toInt())
    }

    @Test
    fun partialDataForIntShortResultsInMinus1() {
        val disInt = DataInputStream(ByteArrayInputStream(byteArrayOf(0x00, 0x00, 0x00)))
        assertEquals(-1, disInt.readInt())
        val disShort = DataInputStream(ByteArrayInputStream(byteArrayOf(0x00)))
        assertEquals((-1).toShort().toInt(), disShort.readShort().toInt())
    }

    @Test
    fun throwsAfterCloseOnReadOperations() {
        val dis = DataInputStream(ByteArrayInputStream(byteArrayOf(1)))
        dis.close()
        assertFailsWith<IllegalStateException> { dis.read() }
        assertFailsWith<IllegalStateException> { dis.read(ByteArray(1)) }
        assertFailsWith<IllegalStateException> { dis.readInt() }
        assertFailsWith<IllegalStateException> { dis.readShort() }
    }
}
