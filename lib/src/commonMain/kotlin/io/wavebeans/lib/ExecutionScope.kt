package io.wavebeans.lib

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

/**
 * [ScopeParameters] are used to bypass some data through [ExecutionScope].
 * You need to serialize the value to a [String] yourself.
 * Hence, it's your responsibility either to convert it back from the [String] representation.
 *
 * This value is stored inside the json specification as you've provided them.
 */
@Suppress("UNCHECKED_CAST")
@Serializable
class ScopeParameters {
    private val params: Map<String, String?>

    constructor() : this(emptyMap())

    constructor(params: Map<String, String?>) {
        this.params = HashMap(params)
    }

    fun add(name: String, value: String): ScopeParameters = ScopeParameters(params + (name to value))
    fun add(name: String, value: Int): ScopeParameters = ScopeParameters(params + (name to value.toString()))
    fun add(name: String, value: Long): ScopeParameters = ScopeParameters(params + (name to value.toString()))
    fun add(name: String, value: Float): ScopeParameters = ScopeParameters(params + (name to value.toString()))
    fun add(name: String, value: Double): ScopeParameters = ScopeParameters(params + (name to value.toString()))
    fun <T : Any> add(name: String, value: Collection<T>, stringifier: (T) -> String): ScopeParameters =
        ScopeParameters(params + (name to value.joinToString(separator = ",") { stringifier(it) }))

    fun <T : Any> addObj(name: String, value: T, stringifier: (T) -> String): ScopeParameters =
        ScopeParameters(params + (name to stringifier(value)))

    fun addStrings(name: String, value: Collection<String>): ScopeParameters = add(name, value) { it }
    fun addInts(name: String, value: Collection<Int>): ScopeParameters = add(name, value) { it.toString() }
    fun addLongs(name: String, value: Collection<Long>): ScopeParameters = add(name, value) { it.toString() }
    fun addFloats(name: String, value: Collection<Float>): ScopeParameters = add(name, value) { it.toString() }
    fun addDoubles(name: String, value: Collection<Double>): ScopeParameters = add(name, value) { it.toString() }

    fun add(name: String, value: ByteArray): ScopeParameters = add(name, Base64.encode(value))

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
        params[name]?.split(",")?.map(objectifier)
}

fun executionScope(block: ScopeParameters.() -> ScopeParameters) = ExecutionScope(ScopeParameters().let(block))

@Serializable
data class ExecutionScope(val parameters: ScopeParameters)

val EmptyScope = ExecutionScope(ScopeParameters())