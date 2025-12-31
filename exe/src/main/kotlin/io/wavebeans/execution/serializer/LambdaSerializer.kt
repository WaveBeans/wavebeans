package io.wavebeans.execution.serializer

import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.toWaveBeansClassLoader
import org.objectweb.asm.*
import kotlin.reflect.jvm.jvmName

interface LambdaWrapper {
    fun <T0, R> serialize(fn: (T0) -> R): String
    fun <T0, T1, R> serialize(fn: (T0, T1) -> R): String
    fun <T0, T1, T2, R> serialize(fn: (T0, T1, T2) -> R): String
    fun <T0, T1, T2, T3, R> serialize(fn: (T0, T1, T2, T3) -> R): String
    fun <T0, R> deserialize(s: String): (T0) -> R
    fun <T0, T1, R> deserialize2(s: String): (T0, T1) -> R
    fun <T0, T1, T2, R> deserialize3(s: String): (T0, T1, T2) -> R
    fun <T0, T1, T2, T3, R> deserialize4(s: String): (T0, T1, T2, T3) -> R
}

var lambdaWrapper: LambdaWrapper = JvmLambdaWrapper()

@Suppress("UNCHECKED_CAST")
class JvmLambdaWrapper : LambdaWrapper {

    override fun <T0, R> serialize(fn: (T0) -> R): String {
        return serializerFn(fn as java.io.Serializable)
    }

    override fun <T0, T1, R> serialize(fn: (T0, T1) -> R): String {
        return serializerFn(fn as java.io.Serializable)
    }

    override fun <T0, T1, T2, R> serialize(fn: (T0, T1, T2) -> R): String {
        return serializerFn(fn as java.io.Serializable)
    }

    override fun <T0, T1, T2, T3, R> serialize(fn: (T0, T1, T2, T3) -> R): String {
        return serializerFn(fn as java.io.Serializable)
    }

    override fun <T0, R> deserialize(s: String): (T0) -> R {
        return deserializeFn(s) as (T0) -> R
    }

    override fun <T0, T1, R> deserialize2(s: String): (T0, T1) -> R {
        return deserializeFn(s) as (T0, T1) -> R
    }

    override fun <T0, T1, T2, R> deserialize3(s: String): (T0, T1, T2) -> R {
        return deserializeFn(s) as? (T0, T1, T2) -> R ?: throw IllegalStateException(
            "Can't deserialize $s to (T0, T1, T2) -> R"
        )
    }

    override fun <T0, T1, T2, T3, R> deserialize4(s: String): (T0, T1, T2, T3) -> R {
        return deserializeFn(s) as (T0, T1, T2, T3) -> R
    }

    private fun deserializeFn(s: String): java.io.Serializable {
        val clazz = WaveBeansClassLoader.classForName(s).java
        val constructor = clazz.declaredConstructors.find { it.parameters.isEmpty() }
        requireNotNull(constructor) {
            "Class $s has no empty constructor, declared ones:\n${
                clazz.declaredConstructors.joinToString("\n") { constructor ->
                    " - " + constructor.toGenericString()
                }
            }\n" + inferDebugOrigin(clazz)
        }
        constructor.isAccessible = true
        return constructor.newInstance() as java.io.Serializable
    }

    private fun serializerFn(fn: java.io.Serializable): String {
        WaveBeansClassLoader.addClassLoader(fn::class.java.classLoader.toWaveBeansClassLoader())
        val className = fn::class.jvmName
        return className
    }
}

private data class DebugOrigin(val sourceFile: String?, val minLine: Int?)

private fun inferDebugOrigin(clazz: Class<*>): DebugOrigin {
    val resourcePath = "/" + clazz.name.replace('.', '/') + ".class"
    val bytes = clazz.getResourceAsStream(resourcePath)?.use { it.readBytes() }
        ?: return DebugOrigin(sourceFile = null, minLine = null)

    var sourceFile: String? = null
    var minLine: Int? = null

    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM9) {
        override fun visitSource(source: String?, debug: String?) {
            sourceFile = source
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            return object : MethodVisitor(Opcodes.ASM9) {
                override fun visitLineNumber(line: Int, start: Label?) {
                    minLine = minLine?.let { kotlin.math.min(it, line) } ?: line
                }
            }
        }
    }, 0)

    return DebugOrigin(sourceFile, minLine)
}