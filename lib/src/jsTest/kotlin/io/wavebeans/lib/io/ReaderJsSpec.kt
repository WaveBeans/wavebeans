package io.wavebeans.lib.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReaderJsSpec {
    @Test
    fun readsUtf8LinesSupportingLfAndCrlf() {
        val bytes = byteArrayOf(
            0x61, 0x0A, // a\n
            0x62, 0x0D, 0x0A, // b\r\n
            0x63, 0xE2.toByte(), 0x82.toByte(), 0xAC.toByte(), 0x0A // c€\n
        )
        val br = ByteArrayInputStream(bytes).bufferedReader()
        val lines = br.lines().toList()
        // expect ["a", "b", "c€"]
        assertEquals(3, lines.size)
        assertEquals("a", lines[0])
        assertEquals("b", lines[1])
        assertEquals("c€", lines[2])
        br.close()
    }

    @Test
    fun readLineThrowsOnEofWhenNoCharactersAreAvailable() {
        val r = ByteArrayInputStream(byteArrayOf()).bufferedReader()
        assertFailsWith<NoSuchElementException> { r.readLine() }
        r.close()
    }
}
