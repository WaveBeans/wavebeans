package io.wavebeans.lib.stream

import io.wavebeans.lib.Sample
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
object SampleCountMeasurement {

    private val types = mutableMapOf<KClass<*>, (Any) -> Int>()

    init {
        registerType(Number::class) { 1 }
        registerType(Sample::class) { 1 }
        registerType(List::class) { l ->
            l.map { samplesInObject(it ?: throw IllegalStateException("List of nullable types is not supported")) }
                    .sum()
        }
    }

    fun <T : Any> registerType(clazz: KClass<T>, measurer: (T) -> Int) {
        check(types.put(clazz, measurer as (Any) -> Int) == null) { "$clazz is already registered" }
    }

    fun samplesInObject(obj: Any): Int {
        return if (obj is Measured)
            obj.measure()
        else types.filterKeys { obj::class.isSubclassOf(it) }
                .map { it.value }
                .firstOrNull()
                ?.invoke(obj)
                ?: throw IllegalStateException("${obj::class} is not registered within ${SampleCountMeasurement::class.simpleName}, use registerType() function " +
                        "or extend your class with ${Measured::class.simpleName} interface")
    }
}
