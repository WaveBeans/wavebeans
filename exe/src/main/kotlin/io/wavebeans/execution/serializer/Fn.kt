package io.wavebeans.execution.serializer

import io.wavebeans.lib.ScopeParameters
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.className
import io.wavebeans.lib.toWaveBeansClassLoader
import io.wavebeans.execution.jsonCompact
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

const val fnClazz = "fnClazz"

interface FnWrapper<T, R> {
    fun wrap(fn: (T) -> R): Fn<T, R>
    fun asString(fn: Fn<T, R>): String
    fun fromString(s: String): Fn<T, R>
    fun instantiate(clazz: KClass<out Fn<T, R>>, initParams: ScopeParameters = ScopeParameters()): Fn<T, R>
}

private val idGenerator = AtomicLong(0)
private val fnRegistry = hashMapOf<Long, Fn<Any?, Any?>>()
private val lambdaRegistry = hashMapOf<Long, (Any?) -> Any?>()

class AnyFn(id: Long) : Fn<Any?, Any?>(ScopeParameters().add("functionId", id)) {
    override fun apply(argument: Any?): Any? {
        TODO()
//        val fid = this.initParams.long("functionId")
//        return lambdaRegistry.getValue(fid).invoke(argument)
    }
}

var fnWrapper: FnWrapper<Any?, Any?> = object : FnWrapper<Any?, Any?> {

    override fun wrap(fn: (Any?) -> Any?): Fn<Any?, Any?> {
        val id = idGenerator.incrementAndGet()
        lambdaRegistry[id] = fn
        return AnyFn(id)
    }

    override fun asString(
        fn: Fn<Any?, Any?>
    ): String {
        val id = idGenerator.incrementAndGet()
        fnRegistry[id] = fn
        return "$fnClazz|$id"
    }

    override fun fromString(s: String): Fn<Any?, Any?> {
        val (fnClazzStr, idStr) = s.split("|")
        require(fnClazzStr == fnClazz) { "Can't deserialize function with class $fnClazzStr" }
        return fnRegistry.getValue(idStr.toLong())
    }

    override fun instantiate(clazz: KClass<out Fn<Any?, Any?>>, initParams: ScopeParameters): Fn<Any?, Any?> {
        require(clazz == AnyFn::class) { "Can't instantiate $clazz" }
        val id = initParams.long("functionId")
        return AnyFn(id)
    }

}

/**
 * Wraps lambda function [fn] to a proper [Fn] class using generic wrapper [WrapFn]. The different between using
 * that method and creating a proper class declaration is that this implementation doesn't allow to by pass parameters
 * as [Fn.initParams] is not available inside lambda function.
 *
 * ```kotlin
 * Fn.wrap { it.doSomethingAndReturn() }
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T, R> wrap(fn: (T) -> R): Fn<T, R> = fnWrapper.wrap(fn as (Any?) -> Any?) as Fn<T, R>

@Suppress("UNCHECKED_CAST")
fun <T1, T2, R> wrap(fn: (T1, T2) -> R): Fn<Pair<T1, T2>, R> =
    fnWrapper.wrap { a ->
        val p = a as Pair<Any?, Any?>
        fn.invoke(p.first as T1, p.second as T2)
    } as Fn<Pair<T1, T2>, R>

@Suppress("UNCHECKED_CAST")
fun <T, R> instantiate(
    clazz: KClass<out Fn<T, R>>,
    initParams: ScopeParameters = ScopeParameters()
): Fn<T, R> = fnWrapper.instantiate(clazz as KClass<out Fn<Any?, Any?>>, initParams) as Fn<T, R>

/**
 * [Fn] is abstract class to launch custom functions. It allows you bypass some parameters to the function execution out
 * of declaration to runtime via using [FnInitParameters]. Each [Fn] is required to have only one (or first) constructor
 * with [FnInitParameters] as the only one parameter.
 *
 * This abstraction exists to be able to separate the declaration tier and runtime tier as there is no way to access declaration
 * tier classes and data if they are not made publicly accessible. For example, it is impossible to use variables which are
 * defined inside inner closure, hence instantiating of [Fn] as inner class is not supported either. [Fn] instance can't
 * have implicit links to outer closure.
 *
 * Mainly that requirement coming from launching the WaveBeans in distributed mode as the single [io.wavebeans.lib.Bean] should be described
 * and then restored on specific environment which differs from local one. Though, if [io.wavebeans.lib.Bean]s run in single thread local
 * mode only, limitations are not that strict and using data out of closures may work.
 *
 * If you don't need to specify any parameters for the function execution, you may use [wrap] method to make the instance.
 * of function out of lamda function.
 */
@Serializable(with = FnSerializer::class)
abstract class Fn<T, R>(val initParams: ScopeParameters = ScopeParameters()) {
    abstract fun apply(argument: T): R
}

@Suppress("UNCHECKED_CAST")
object FnSerializer : KSerializer<Fn<*, *>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(Fn::class.className()) {
        element("fnClass", String.serializer().descriptor)
        element("initParams", ScopeParameters.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Fn<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var initParams: ScopeParameters
            lateinit var fnClazz: KClass<Fn<Any, Any>>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> fnClazz =
                        WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i)) as KClass<Fn<Any, Any>>

                    1 -> initParams = decodeSerializableElement(descriptor, i, ScopeParameters.serializer())
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            instantiate(fnClazz, initParams)
        }
    }

    override fun serialize(encoder: Encoder, value: Fn<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value::class.className())
            encodeSerializableElement(descriptor, 1, ScopeParameters.serializer(), value.initParams)
        }
    }
}

class JvmFnWrapper<T, R> : FnWrapper<T, R> {
    /**
     * Wraps lambda function [fn] to a proper [io.wavebeans.execution.serializer.Fn] class using generic wrapper [WrapFn]. The different between using
     * that method and creating a proper class declaration is that this implementation doesn't allow to by pass parameters
     * as [initParams] is not available inside lambda function.
     *
     * ```kotlin
     * Fn.wrap { it.doSomethingAndReturn() }
     * ```
     */
    override fun wrap(fn: (T) -> R): Fn<T, R> {
        WaveBeansClassLoader.addClassLoader(fn::class.java.classLoader.toWaveBeansClassLoader())
        return WrapFn(ScopeParameters().add(fnClazz, fn::class.jvmName))
    }

    override fun asString(fn: Fn<T, R>): String {
        return jsonCompact.encodeToString<Fn<T, R>>(fn)
    }

    @Suppress("UNCHECKED_CAST")
    override fun fromString(s: String): Fn<T, R> {
        return jsonCompact.decodeFromString<Fn<T, R>>(s)
    }

    @Suppress("UNCHECKED_CAST")
    override fun instantiate(
        clazz: KClass<out Fn<T, R>>,
        initParams: ScopeParameters
    ): Fn<T, R> {
        val jClazz = clazz.java
        return jClazz.declaredConstructors
            .firstOrNull { with(it.parameterTypes) { size == 1 && get(0).isAssignableFrom(ScopeParameters::class.java) } }
            .let { it ?: jClazz.declaredConstructors.firstOrNull { c -> c.parameters.isEmpty() } }
            ?.also { it.isAccessible = true }
            ?.let { c ->
                if (c.parameters.size == 1)
                    c.newInstance(initParams)
                else
                    c.newInstance()
            }
            ?.let { it as Fn<T, R> }
            ?: throw IllegalStateException(
                "$clazz has no proper constructor with ${ScopeParameters::class} as only one parameter or empty at all, " +
                        "it has: ${jClazz.declaredConstructors.joinToString { it.parameterTypes.toList().toString() }}"
            )
    }

}

/**
 * Helper [io.wavebeans.execution.serializer.Fn] to wrap lambda functions within [io.wavebeans.execution.serializer.Fn] instance to provide more friendly API.
 */
@Suppress("UNCHECKED_CAST")
internal class WrapFn<T, R>(initParams: ScopeParameters) : Fn<T, R>(initParams) {

    private val fn: (T) -> R

    init {
        val clazzName = initParams[fnClazz]!!
        try {
            val clazz = WaveBeansClassLoader.classForName(clazzName)
            val constructor = clazz.java.declaredConstructors.first()
            constructor.isAccessible = true
            fn = constructor.newInstance() as (T) -> R
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Wrapping function $clazzName failed, perhaps it is implemented as inner class" +
                        " and should be wrapped manually", e
            )
        }
    }

    override fun apply(argument: T): R {
        return fn(argument)
    }
}
