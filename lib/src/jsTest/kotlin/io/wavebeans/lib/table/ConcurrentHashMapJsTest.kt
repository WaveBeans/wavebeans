package io.wavebeans.lib.table

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ConcurrentHashMapJsTest {
    @Test
    fun shouldWorkAsAMap() {
        val map = ConcurrentHashMap<String, Int>()
        map["a"] = 1
        map["b"] = 2
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(2, map.size)
        assertTrue(map.containsKey("a"))
        assertFalse(map.containsKey("c"))
    }

    @Test
    fun shouldSupportPutIfAbsent() {
        val map = ConcurrentHashMap<String, Int>()
        val old1 = map.putIfAbsent("a", 1)
        assertEquals(null, old1)
        assertEquals(1, map["a"])

        val old2 = map.putIfAbsent("a", 2)
        assertEquals(1, old2)
        assertEquals(1, map["a"])
    }

    @Test
    fun shouldSupportRemoveAndClear() {
        val map = ConcurrentHashMap<String, Int>()
        map["a"] = 1
        assertEquals(1, map.remove("a"))
        assertTrue(map.isEmpty())

        map["b"] = 2
        map.clear()
        assertEquals(0, map.size)
    }
}
