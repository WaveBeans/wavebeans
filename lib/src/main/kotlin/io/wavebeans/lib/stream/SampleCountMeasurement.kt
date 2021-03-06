package io.wavebeans.lib.stream

import io.wavebeans.lib.Sample
import io.wavebeans.lib.SampleVector
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

@Suppress("UNCHECKED_CAST")
object SampleCountMeasurement {

    private val types = mutableMapOf<KClass<*>, (Any) -> Int>()

    init {
        registerType(Number::class) { 1 }
        registerType(Sample::class) { 1 }
        registerType(SampleVector::class) { it.size }
        registerType(Pair::class) { max(samplesInObject(it.first!!), samplesInObject(it.second!!)) }
        registerType(List::class) { l ->
            l.asSequence()
                    .map {
                        samplesInObject(it ?: throw IllegalStateException("List of nullable types is not supported"))
                    }
                    .sum()
        }
    }

    fun <T : Any> registerType(clazz: KClass<T>, measurer: (T) -> Int) {
        check(types.put(clazz, measurer as (Any) -> Int) == null) { "$clazz is already registered" }
    }

    private val cache = ConcurrentHashMap<String, (Any) -> Int>()

    fun samplesInObject(obj: Any): Int {
        return if (obj is Measured)
            obj.measure()
        else {
            val measurer = cache[obj::class.jvmName]
                    ?: types.entries.firstOrNull { obj::class.isSubclassOf(it.key) }
                            ?.also { cache[it.key.jvmName] = it.value }
                            ?.value
                    ?: throw IllegalStateException("${obj::class} is not registered within ${SampleCountMeasurement::class.simpleName}," +
                            " use registerType() function or extend your class with ${Measured::class.simpleName} interface")

            measurer.invoke(obj)
        }
    }
}
