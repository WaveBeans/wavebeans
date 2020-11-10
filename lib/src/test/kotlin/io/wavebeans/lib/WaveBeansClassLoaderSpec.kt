package io.wavebeans.lib

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import io.wavebeans.lib.WaveBeansClassLoader.classForName
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.jvm.jvmName

object WaveBeansClassLoaderSpec : Spek({

    describe("Load default classes") {

        describe("Primitives") {
            it("should load byte") { assertThat(classForName(Byte::class.jvmName)).isEqualTo(Byte::class.java) }
            it("should load short") { assertThat(classForName(Short::class.jvmName)).isEqualTo(Short::class.java) }
            it("should load int") { assertThat(classForName(Int::class.jvmName)).isEqualTo(Int::class.java) }
            it("should load long") { assertThat(classForName(Long::class.jvmName)).isEqualTo(Long::class.java) }
            it("should load double") { assertThat(classForName(Double::class.jvmName)).isEqualTo(Double::class.java) }
            it("should load ByteArray") { assertThat(classForName(ByteArray::class.jvmName)).isEqualTo(ByteArray::class.java) }
            it("should load ShortArray") { assertThat(classForName(ShortArray::class.jvmName)).isEqualTo(ShortArray::class.java) }
            it("should load IntArray") { assertThat(classForName(IntArray::class.jvmName)).isEqualTo(IntArray::class.java) }
            it("should load LongArray") { assertThat(classForName(LongArray::class.jvmName)).isEqualTo(LongArray::class.java) }
            it("should load FloatArray") { assertThat(classForName(FloatArray::class.jvmName)).isEqualTo(FloatArray::class.java) }
            it("should load DoubleArray") { assertThat(classForName(DoubleArray::class.jvmName)).isEqualTo(DoubleArray::class.java) }
        }

        describe("Collections") {
            it("should load set") { assertThat(classForName(Set::class.jvmName)).isEqualTo(Set::class.java) }
            it("should load map") { assertThat(classForName(Map::class.jvmName)).isEqualTo(Map::class.java) }
            it("should load list") { assertThat(classForName(List::class.jvmName)).isEqualTo(List::class.java) }
        }

        describe("Builtin classes") {
            it("should load Sample") { assertThat(classForName(Sample::class.jvmName)).isEqualTo(Sample::class.java) }
            it("should load SampleVector") { assertThat(classForName(SampleVector::class.jvmName)).isEqualTo(SampleVector::class.java) }
            it("should load FftSample") { assertThat(classForName(FftSample::class.jvmName)).isEqualTo(FftSample::class.java) }
            it("should load Window") { assertThat(classForName(Window::class.jvmName)).isEqualTo(Window::class.java) }
        }
    }

    describe("Load external classes") {
        val className = "my.namespace.MyClass$1_lambda1234"
        val classLoader by memoized(CachingMode.SCOPE) {
            object : ClassLoader() {
                override fun loadClass(name: String?): Class<*> {
                    if (name == className) throw Exception(className)
                    return super.loadClass(name)
                }
            }
        }
        it("should throw exception for non-existing class") {
            assertThat(catch { classForName(className) })
                    .isNotNull()
                    .isInstanceOf(ClassNotFoundException::class)
                    .message().isNotNull().contains(className)
        }

        it("should load class provided by registered classloader") {

            WaveBeansClassLoader.addClassLoader(classLoader)

            assertThat(catch { classForName(className) })
                    .isNotNull()
                    .message().isNotNull().isEqualTo(className)
        }

        it("should not load class if class loader unregistered") {

            WaveBeansClassLoader.removeClassLoader(classLoader)

            assertThat(catch { classForName(className) })
                    .isNotNull()
                    .isInstanceOf(ClassNotFoundException::class)
                    .message().isNotNull().contains(className)
        }
    }
})