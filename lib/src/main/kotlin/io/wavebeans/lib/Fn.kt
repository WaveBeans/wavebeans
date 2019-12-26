package io.wavebeans.lib

import kotlinx.serialization.*
import kotlinx.serialization.internal.HashMapSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.nullable
import kotlin.reflect.jvm.jvmName

/**
 * [Fn] is abstract class to launch custom functions. It allows you bypass some parameters to the function execution out
 * of declaration to runtime via using [FnInitParameters]. Each [Fn] is required to have only one (or first) constructor
 * with [FnInitParameters] as the only one parameter.
 *
 * This abstract exist to be able to separate the declaration tier and runtime tier as there is no way to access declaration
 * tier classes and data if they are not made publicly accessible. For example, it is impossible to use variables which are
 * defined inside inner closure, hence instantiating of [Fn] as inner class is not supported either. [Fn] instance can't
 * have implicit links to outer closure.
 *
 * Mainly that requirement coming from launching the WaveBeans in distributed mode as the single [Bean] should be described
 * and then restored on specific environment which differs from local one. Though, if [Bean]s run in single thread local
 * mode only, limitations are not that strict and using data out of closures may work.
 */
@Serializable(with = FnSerializer::class)
@FunctionalInterface
abstract class Fn<T, R>(val initParams: FnInitParameters = FnInitParameters()) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T, R> instantiate(clazz: Class<Fn<T, R>>, initParams: FnInitParameters = FnInitParameters()): Fn<T, R> {
            return clazz.declaredConstructors.first {
                it.parameterTypes.size == 1 &&
                        it.parameterTypes[0].isAssignableFrom(FnInitParameters::class.java)
            }
                    .also { it.isAccessible = true }
                    .newInstance(initParams) as Fn<T, R>
        }

        fun <T, R> wrap(fn: (T) -> R): Fn<T, R> {
            return WrapFn(FnInitParameters().add("fnClazz", fn::class.jvmName))
        }
    }

    abstract fun apply(argument: T): R

}

/**
 * [FnInitParameters] are used to bypass some data to [Fn]. You need to serialize the value to a [String] yourself.
 * Hence, it's your responsibility either to convert it back from the [String] representation. The value is nullable.
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

    fun add(name: String, value: String?): FnInitParameters = FnInitParameters(params + (name to value))

    operator fun get(name: String): String? = params[name]
}

/**
 * Helper [Fn] to wrap lambda functions within [Fn] instance to provide more friendly API.
 */
@Suppress("UNCHECKED_CAST")
internal class WrapFn<T, R>(initParams: FnInitParameters) : Fn<T, R>(initParams) {

    private val fn: (T) -> R

    init {
        val clazz = Class.forName(initParams["fnClazz"]!!)
        val constructor = clazz.declaredConstructors.first()
        constructor.isAccessible = true
        fn = constructor.newInstance() as (T) -> R
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
                0 -> fnClazz = Class.forName(dec.decodeStringElement(descriptor, i)) as Class<Fn<Any, Any>>
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