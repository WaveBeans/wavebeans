package io.wavebeans.lib

import kotlinx.atomicfu.atomic
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.io.encoding.Base64
import kotlin.reflect.KClass

const val fnClazz = "fnClazz"

interface FnWrapper<T, R> {
    fun wrap(fn: (T) -> R): Fn<T, R>
    fun asString(fn: Fn<T, R>): String
    fun fromString(s: String): Fn<T, R>
    fun instantiate(clazz: KClass<out Fn<T, R>>, initParams: FnInitParameters = FnInitParameters()): Fn<T, R>
}

private val idGenerator = atomic(0L)
private val fnRegistry = hashMapOf<Long, Fn<Any?, Any?>>()
private val lambdaRegistry = hashMapOf<Long, (Any?) -> Any?>()

class AnyFn(id: Long) : Fn<Any?, Any?>(FnInitParameters().add("functionId", id)) {
    override fun apply(argument: Any?): Any? {
        val fid = this.initParams.long("functionId")
        return lambdaRegistry.getValue(fid).invoke(argument)
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

    override fun instantiate(clazz: KClass<out Fn<Any?, Any?>>, initParams: FnInitParameters): Fn<Any?, Any?> {
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
    initParams: FnInitParameters = FnInitParameters()
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
 * Mainly that requirement coming from launching the WaveBeans in distributed mode as the single [Bean] should be described
 * and then restored on specific environment which differs from local one. Though, if [Bean]s run in single thread local
 * mode only, limitations are not that strict and using data out of closures may work.
 *
 * If you don't need to specify any parameters for the function execution, you may use [wrap] method to make the instance.
 * of function out of lamda function.
 */
@Serializable(with = FnSerializer::class)
abstract class Fn<T, R>(val initParams: FnInitParameters = FnInitParameters()) {
    abstract fun apply(argument: T): R
}

/**
 * [FnInitParameters] are used to bypass some data to [Fn]. You need to serialize the value to a [String] yourself.
 * Hence, it's your responsibility either to convert it back from the [String] representation.
 *
 * This value is stored inside the json specification as you've provided them.
 */
@Suppress("UNCHECKED_CAST")
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
    fun add(name: String, value: Fn<*, *>): FnInitParameters =
        addObj(name, value) { fnWrapper.asString(value as Fn<Any?, Any?>) }

    fun add(name: String, value: ByteArray): FnInitParameters = add(name, Base64.encode(value))

    operator fun get(name: String): String? = params[name]
    fun notNull(name: String): String = params[name] ?: throw IllegalArgumentException("Parameters $name is null")

    fun <T : Any> obj(name: String, objectifier: (String) -> T): T = notNull(name).let(objectifier)
    fun <T : Any> objOrNull(name: String, objectifier: (String) -> T): T? = get(name)?.let(objectifier)

    fun <T : Any, R : Any> fn(name: String): Fn<T, R> = obj(name) { fnWrapper.fromString(it) as Fn<T, R> }
    fun <T : Any, R : Any> fnOrNull(name: String): Fn<T, R>? = objOrNull(name) { fnWrapper.fromString(it) as Fn<T, R> }

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
        params[name]?.split(",")?.map(objectifier)
}

object FnInitParametersSerializer : KSerializer<FnInitParameters> {

    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FnInitParameters::class.className()) {
        element("parametersMap", mapSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): FnInitParameters {
        return decoder.decodeStructure(descriptor) {
            lateinit var params: Map<String, String>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> params = decodeSerializableElement(
                        descriptor,
                        i,
                        mapSerializer
                    )

                    else -> throw SerializationException("Unknown index $i")
                }
            }
            FnInitParameters(params)
        }
    }

    override fun serialize(encoder: Encoder, value: FnInitParameters) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(
                descriptor,
                0,
                MapSerializer(String.serializer(), String.serializer().nullable),
                value.params
            )
        }
    }

}

@Suppress("UNCHECKED_CAST")
object FnSerializer : KSerializer<Fn<*, *>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(Fn::class.className()) {
        element("fnClass", String.serializer().descriptor)
        element("initParams", FnInitParametersSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): Fn<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var initParams: FnInitParameters
            lateinit var fnClazz: KClass<Fn<Any, Any>>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> fnClazz =
                        WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i)) as KClass<Fn<Any, Any>>

                    1 -> initParams = decodeSerializableElement(descriptor, i, FnInitParameters.serializer())
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            instantiate(fnClazz, initParams)
        }
    }

    override fun serialize(encoder: Encoder, value: Fn<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value::class.className())
            encodeSerializableElement(descriptor, 1, FnInitParametersSerializer, value.initParams)
        }
    }
}
