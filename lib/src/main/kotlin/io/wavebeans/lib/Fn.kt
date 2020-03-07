package io.wavebeans.lib

import kotlinx.serialization.*
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.nullable
import kotlin.reflect.jvm.jvmName

private const val fnClazz = "fnClazz"

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
 * Mainly that requirement coming from launching the WaveBeans in distributed mode as the single [Bean] should be described
 * and then restored on specific environment which differs from local one. Though, if [Bean]s run in single thread local
 * mode only, limitations are not that strict and using data out of closures may work.
 */
@Serializable(with = FnSerializer::class)
abstract class Fn<T, R>(val initParams: FnInitParameters = FnInitParameters()) {

    companion object {

        /**
         * Instantiate [Fn] of specified [clazz] initiailizing with [initParams]. Searches for the constructor with only
         * one parameter of type [FnInitParameters].
         *
         * @throws [IllegalStateException] if constructor with only parameter og [FnInitParameters] is not found.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T, R> instantiate(clazz: Class<out Fn<T, R>>, initParams: FnInitParameters = FnInitParameters()): Fn<T, R> {
            return clazz.declaredConstructors
                    .firstOrNull { with(it.parameterTypes) { size == 1 && get(0).isAssignableFrom(FnInitParameters::class.java) } }
                    .let { it ?: clazz.declaredConstructors.firstOrNull { c -> c.parameters.isEmpty() } }
                    ?.also { it.isAccessible = true }
                    ?.let { c ->
                        if (c.parameters.size == 1)
                            c.newInstance(initParams)
                        else
                            c.newInstance()
                    }
                    ?.let { it as Fn<T, R> }
                    ?: throw IllegalStateException("$clazz has no proper constructor with ${FnInitParameters::class} as only one parameter or empty at all, " +
                            "it has: ${clazz.declaredConstructors.joinToString { it.parameterTypes.toList().toString() }}")
        }

        /**
         * Wraps lambda function [fn] to a proper [Fn] class using generic wrapper [WrapFn]. The different between using
         * that method and creating a proper class declaration is that this implementation doesn't allow to by pass parameters
         * as [initParams] is not available inside lambda function.
         *
         * ```kotlin
         * Fn.wrap { it.doSomethingAndReturn() }
         * ```
         */
        fun <T, R> wrap(fn: (T) -> R): Fn<T, R> {
            return WrapFn(FnInitParameters().add(fnClazz, fn::class.jvmName))
        }
    }

    abstract fun apply(argument: T): R

}

/**
 * [FnInitParameters] are used to bypass some data to [Fn]. You need to serialize the value to a [String] yourself.
 * Hence, it's your responsibility either to convert it back from the [String] representation.
 *
 * This value is stored inside the json specification as you've provided them.
 */
@Serializable(with = FnInitParametersSerializer::class)
class FnInitParameters {

    constructor() : this(emptyMap())

    val params: Map<String, String?>

    constructor(params: Map<String, String?>) {
        this.params = HashMap(params)
    }

    fun add(name: String, value: String): FnInitParameters = FnInitParameters(params + (name to value))
    fun add(name: String, value: Int): FnInitParameters = FnInitParameters(params + (name to value.toString()))
    fun add(name: String, value: Long): FnInitParameters = FnInitParameters(params + (name to value.toString()))
    fun add(name: String, value: Float): FnInitParameters = FnInitParameters(params + (name to value.toString()))
    fun add(name: String, value: Double): FnInitParameters = FnInitParameters(params + (name to value.toString()))
    fun <T : Any> add(name: String, value: Collection<T>, stringifier: (T) -> String): FnInitParameters =
            FnInitParameters(params + (name to value.joinToString(separator = ",") { stringifier(it) }))

    fun <T : Any> addObj(name: String, value: T, stringifier: (T) -> String): FnInitParameters =
            FnInitParameters(params + (name to stringifier(value)))

    fun addStrings(name: String, value: Collection<String>): FnInitParameters = add(name, value) { it }
    fun addInts(name: String, value: Collection<Int>): FnInitParameters = add(name, value) { it.toString() }
    fun addLongs(name: String, value: Collection<Long>): FnInitParameters = add(name, value) { it.toString() }
    fun addFloats(name: String, value: Collection<Float>): FnInitParameters = add(name, value) { it.toString() }
    fun addDoubles(name: String, value: Collection<Double>): FnInitParameters = add(name, value) { it.toString() }

    operator fun get(name: String): String? = params[name]
    fun notNull(name: String): String = params[name] ?: throw IllegalArgumentException("Parameters $name is null")

    fun <T : Any> obj(name: String, objectifier: (String) -> T): T = notNull(name).let(objectifier)
    fun <T : Any> objOrNull(name: String, objectifier: (String) -> T): T? = get(name)?.let(objectifier)

    fun string(name: String): String = notNull(name)
    fun stringOrNull(name: String): String? = get(name)
    fun strings(name: String): List<String> = list(name) { it }
    fun stringsOrNull(name: String): List<String>? = listOrNull(name) { it }

    fun int(name: String): Int = notNull(name).toInt()
    fun intOrNull(name: String): Int? = get(name)?.toInt()
    fun ints(name: String): List<Int> = list(name) { it.toInt() }
    fun intsOrNull(name: String): List<Int>? = listOrNull(name) { it.toInt() }


    fun long(name: String): Long = notNull(name).toLong()
    fun longOrNull(name: String): Long? = get(name)?.toLong()
    fun longs(name: String): List<Long> = list(name) { it.toLong() }
    fun longsOrNull(name: String): List<Long>? = listOrNull(name) { it.toLong() }

    fun float(name: String): Float = notNull(name).toFloat()
    fun floatOrNull(name: String): Float? = get(name)?.toFloat()
    fun floats(name: String): List<Float> = list(name) { it.toFloat() }
    fun floatsOrNull(name: String): List<Float>? = listOrNull(name) { it.toFloat() }

    fun double(name: String): Double = notNull(name).toDouble()
    fun doubleOrNull(name: String): Double? = get(name)?.toDouble()
    fun doubles(name: String): List<Double> = list(name) { it.toDouble() }
    fun doublesOrNull(name: String): List<Double>? = listOrNull(name) { it.toDouble() }

    fun <T : Any> list(name: String, objectifier: (String) -> T): List<T> = listOrNull(name, objectifier)
            ?: throw IllegalArgumentException("Parameters $name is null")

    fun <T : Any> listOrNull(name: String, objectifier: (String) -> T): List<T>? =
            params[name]?.let { it.split(",").map(objectifier) }
}

/**
 * Helper [Fn] to wrap lambda functions within [Fn] instance to provide more friendly API.
 */
@Suppress("UNCHECKED_CAST")
class WrapFn<T, R>(initParams: FnInitParameters) : Fn<T, R>(initParams) {

    private val fn: (T) -> R

    init {
        val clazzName = initParams[fnClazz]!!
        try {
            val clazz = WaveBeansClassLoader.classForName(clazzName)
            val constructor = clazz.declaredConstructors.first()
            constructor.isAccessible = true
            fn = constructor.newInstance() as (T) -> R
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Wrapping function $clazzName failed, perhaps it is implemented as inner class" +
                    " and should be wrapped manually", e)
        }
    }

    override fun apply(argument: T): R {
        return fn(argument)
    }

}

object FnInitParametersSerializer : KSerializer<FnInitParameters> {
    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("FnInitParameters") {
        init {
            addElement("parametersMap")
        }
    }

    override fun deserialize(decoder: Decoder): FnInitParameters {
        val dec = decoder.beginStructure(descriptor)
        var params: Map<String, String>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> params = dec.decodeSerializableElement(
                        descriptor,
                        i,
                        HashMapSerializer(StringSerializer, StringSerializer)
                )
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return FnInitParameters(params!!)
    }

    override fun serialize(encoder: Encoder, obj: FnInitParameters) {
        val s = encoder.beginStructure(descriptor)
        s.encodeSerializableElement(
                descriptor,
                0,
                HashMapSerializer(StringSerializer, StringSerializer.nullable),
                obj.params
        )
        s.endStructure(descriptor)
    }

}

@Suppress("UNCHECKED_CAST")
object FnSerializer : KSerializer<Fn<*, *>> {
    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("FnSerializer") {
        init {
            addElement("fnClass")
            addElement("initParams")
        }
    }

    override fun deserialize(decoder: Decoder): Fn<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var initParams: FnInitParameters? = null
        var fnClazz: Class<Fn<Any, Any>>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> fnClazz = WaveBeansClassLoader.classForName(dec.decodeStringElement(descriptor, i)) as Class<Fn<Any, Any>>
                1 -> initParams = dec.decodeSerializableElement(descriptor, i, FnInitParameters.serializer())
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return Fn.instantiate(fnClazz!!, initParams!!)
    }

    override fun serialize(encoder: Encoder, obj: Fn<*, *>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, obj::class.jvmName)
        structure.encodeSerializableElement(descriptor, 1, FnInitParametersSerializer, obj.initParams)
        structure.endStructure(descriptor)
    }

}