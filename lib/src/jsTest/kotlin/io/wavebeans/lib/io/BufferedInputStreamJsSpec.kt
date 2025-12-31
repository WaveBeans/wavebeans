package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferedInputStreamJsSpec {
    @Test
    fun delegatesSingleAndBulkReadsPreservesEofAndThrowsAfterClose() {
        val base = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val bis = BufferedInputStream(base)
        assertEquals(1, bis.read())
        val buf = ByteArray(4)
        val n = bis.read(buf, 1, 2)
        assertEquals(2, n)
        // remaining bytes 2,3 should be in positions 1,2
        assertEquals(2, buf[1].toInt())
        assertEquals(3, buf[2].toInt())
        assertEquals(-1, bis.read())
        bis.close()
        assertFailsWith<IllegalStateException> { bis.read() }
    }
}
