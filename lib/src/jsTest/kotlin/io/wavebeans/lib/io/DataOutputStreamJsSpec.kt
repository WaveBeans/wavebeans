package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DataOutputStreamJsSpec {
    @Test
    fun writeIntWritesBigEndianBytes() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeInt(0x01020304)
        val arr = baos.toByteArray()
        assertTrue(arr.contentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04)))
    }

    @Test
    fun writeShortWritesBigEndianLow16Bits() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeShort(0xABCD)
        val arr = baos.toByteArray()
        assertTrue(arr.contentEquals(byteArrayOf(0xAB.toByte(), 0xCD.toByte())))
    }

    @Test
    fun delegatesWriteAndWriteBufferOffLen() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.write(0xFF)
        dos.write(byteArrayOf(1, 2, 3))
        dos.write(byteArrayOf(9, 8, 7, 6), 1, 2)
        val arr = baos.toByteArray()
        assertTrue(arr.contentEquals(byteArrayOf(0xFF.toByte(), 1, 2, 3, 8, 7)))
    }

    @Test
    fun throwsAfterCloseOnWriteOperations() {
        val dos = DataOutputStream(ByteArrayOutputStream())
        dos.close()
        assertFailsWith<IllegalStateException> { dos.write(1) }
        assertFailsWith<IllegalStateException> { dos.write(byteArrayOf(1)) }
        assertFailsWith<IllegalStateException> { dos.writeShort(1) }
        assertFailsWith<IllegalStateException> { dos.writeInt(1) }
    }
}
