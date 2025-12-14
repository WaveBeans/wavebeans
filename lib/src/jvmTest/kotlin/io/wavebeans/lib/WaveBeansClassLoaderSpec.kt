package io.wavebeans.lib

import assertk.assertThat
import assertk.assertions.*
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.lib.WaveBeansClassLoader.classForName
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class WaveBeansClassLoaderSpec : DescribeSpec({

    describe("Load default classes") {

        describe("Primitives") {
            it("should load byte") { assertThat(classForName(Byte::class.jvmName)).isEqualTo(Byte::class) }
            it("should load short") { assertThat(classForName(Short::class.jvmName)).isEqualTo(Short::class) }
            it("should load int") { assertThat(classForName(Int::class.jvmName)).isEqualTo(Int::class) }
            it("should load long") { assertThat(classForName(Long::class.jvmName)).isEqualTo(Long::class) }
            it("should load double") { assertThat(classForName(Double::class.jvmName)).isEqualTo(Double::class) }
            it("should load ByteArray") { assertThat(classForName(ByteArray::class.jvmName)).isEqualTo(ByteArray::class) }
            it("should load ShortArray") { assertThat(classForName(ShortArray::class.jvmName)).isEqualTo(ShortArray::class) }
            it("should load IntArray") { assertThat(classForName(IntArray::class.jvmName)).isEqualTo(IntArray::class) }
            it("should load LongArray") { assertThat(classForName(LongArray::class.jvmName)).isEqualTo(LongArray::class) }
            it("should load FloatArray") { assertThat(classForName(FloatArray::class.jvmName)).isEqualTo(FloatArray::class) }
            it("should load DoubleArray") { assertThat(classForName(DoubleArray::class.jvmName)).isEqualTo(DoubleArray::class) }
        }

        describe("Collections") {
            it("should load set") { assertThat(classForName(Set::class.jvmName)).isEqualTo(Set::class) }
            it("should load map") { assertThat(classForName(Map::class.jvmName)).isEqualTo(Map::class) }
            it("should load list") { assertThat(classForName(List::class.jvmName)).isEqualTo(List::class) }
        }

        describe("Builtin classes") {
            it("should load Sample") { assertThat(classForName(Sample::class.jvmName)).isEqualTo(Sample::class) }
            it("should load SampleVector") { assertThat(classForName(SampleVector::class.jvmName)).isEqualTo(
                SampleVector::class) }
            it("should load FftSample") { assertThat(classForName(FftSample::class.jvmName)).isEqualTo(FftSample::class) }
            it("should load Window") { assertThat(classForName(Window::class.jvmName)).isEqualTo(Window::class) }
        }
    }

    describe("Load external classes") {
        val className = "my.namespace.MyClass$1_lambda1234"

        fun newClassLoader(): ClassLoader = object : ClassLoader {

            override fun classForName(name: String): KClass<*> {
                if (name == className) throw Exception(className)
                return Class.forName(name).kotlin
            }
        }

        it("should throw exception for non-existing class") {
            assertThat( runCatching{ classForName(className) } )
                .isFailure()
                .isNotNull()
                .isInstanceOf(ClassNotFoundException::class)
                .message().isNotNull().contains(className)
        }

        it("should load class provided by registered classloader") {
            val classLoader = newClassLoader()
            WaveBeansClassLoader.addClassLoader(classLoader)
            try {
                assertThat( runCatching{ classForName(className) } )
                    .isFailure()
                    .isNotNull()
                    .message().isNotNull().isEqualTo(className)
            } finally {
                WaveBeansClassLoader.removeClassLoader(classLoader)
            }
        }

        it("should not load class if class loader unregistered") {
            val classLoader = newClassLoader()
            WaveBeansClassLoader.addClassLoader(classLoader)
            WaveBeansClassLoader.removeClassLoader(classLoader)

            assertThat( runCatching{ classForName(className) } )
                .isFailure()
                .isNotNull()
                .isInstanceOf(ClassNotFoundException::class)
                .message().isNotNull().contains(className)
        }
    }
})