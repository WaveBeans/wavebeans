package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class BufferedOutputStreamJsSpec {
    @Test
    fun buffersSmallWritesAndFlushesOnFlushAndClose() {
        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos, 4)
        bos.write(byteArrayOf(1, 2)) // stays in buffer
        // Not flushed yet
        assertTrue(baos.toByteArray().isEmpty())
        bos.flush()
        assertTrue(baos.toByteArray().contentEquals(byteArrayOf(1, 2)))

        bos.write(byteArrayOf(3, 4))
        bos.close() // should flush remaining
        assertTrue(baos.toByteArray().contentEquals(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun writesFullBufferSizedChunksDirectlyWhenInputExceedsBuffer() {
        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos, 4)
        bos.write(byteArrayOf(9, 8, 7, 6, 5))
        // first 4 bytes should have gone through directly, last 1 byte buffered
        assertTrue(baos.toByteArray().contentEquals(byteArrayOf(9, 8, 7, 6)))
        bos.flush()
        assertTrue(baos.toByteArray().contentEquals(byteArrayOf(9, 8, 7, 6, 5)))
    }

    @Test
    fun supportsMixedSingleByteAndBulkWrites() {
        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos, 3)
        bos.write(0xFF)
        bos.write(byteArrayOf(1, 2)) // buffer now full
        // Should flush automatically on next write if needed
        bos.write(byteArrayOf(3, 4, 5)) // writes a chunk directly (size >= buf), then buffers remainder
        bos.flush()
        assertTrue(baos.toByteArray().contentEquals(byteArrayOf(0xFF.toByte(), 1, 2, 3, 4, 5)))
    }

    @Test
    fun throwsAfterCloseOnWriteOperations() {
        val bos = BufferedOutputStream(ByteArrayOutputStream(), 4)
        bos.close()
        assertFailsWith<IllegalStateException> { bos.write(1) }
        assertFailsWith<IllegalStateException> { bos.write(byteArrayOf(1)) }
    }
}
