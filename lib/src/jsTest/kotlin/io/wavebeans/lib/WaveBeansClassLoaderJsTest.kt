package io.wavebeans.lib

import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaveBeansClassLoaderJsTest {

    @BeforeTest
    fun setup() {
        WaveBeansClassLoader.reset()
    }

    @Test
    fun shouldResolvePrimitives() {
        assertEquals(Int::class, WaveBeansClassLoader.classForName("int"))
        assertEquals(Int::class, WaveBeansClassLoader.classForName("kotlin.Int"))
        assertEquals(Long::class, WaveBeansClassLoader.classForName("long"))
        assertEquals(ByteArray::class, WaveBeansClassLoader.classForName("ByteArray"))
        assertEquals(Any::class, WaveBeansClassLoader.classForName("Any"))
    }

    @Test
    fun shouldResolveClassesViaRegisteredLoaders() {
        val myClassLoader = object : ClassLoader {
            override fun classForName(name: String): KClass<*> {
                return if (name == "MyClass") WaveBeansClassLoaderJsTest::class else throw Exception("Not found")
            }
        }
        WaveBeansClassLoader.addClassLoader(myClassLoader)
        assertEquals(WaveBeansClassLoaderJsTest::class, WaveBeansClassLoader.classForName("MyClass"))
    }

    @Test
    fun shouldFailIfClassNotFound() {
        assertFailsWith<Exception> {
            WaveBeansClassLoader.classForName("NonExistentClass")
        }
    }

    @Test
    fun shouldRemoveClassLoader() {
        val myClassLoader = object : ClassLoader {
            override fun classForName(name: String): KClass<*> {
                return if (name == "MyClass") WaveBeansClassLoaderJsTest::class else throw Exception("Not found")
            }
        }
        WaveBeansClassLoader.addClassLoader(myClassLoader)
        WaveBeansClassLoader.removeClassLoader(myClassLoader)
        assertFailsWith<Exception> {
            WaveBeansClassLoader.classForName("MyClass")
        }
    }
}
