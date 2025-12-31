package io.wavebeans.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class ReflectionUtilsJsTest {
    @Test
    fun shouldReturnSimpleNameForClass() {
        assertEquals("String", String::class.className())
        assertEquals("Int", Int::class.className())
        assertEquals("ReflectionUtilsJsTest", ReflectionUtilsJsTest::class.className())
    }
}
