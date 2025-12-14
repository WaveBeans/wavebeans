package io.wavebeans.lib

import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class JvmFnWrapper<T, R> : FnWrapper<T, R> {
    /**
     * Wraps lambda function [fn] to a proper [Fn] class using generic wrapper [WrapFn]. The different between using
     * that method and creating a proper class declaration is that this implementation doesn't allow to by pass parameters
     * as [initParams] is not available inside lambda function.
     *
     * ```kotlin
     * Fn.wrap { it.doSomethingAndReturn() }
     * ```
     */
    override fun wrap(fn: (T) -> R): Fn<T, R> {
        WaveBeansClassLoader.addClassLoader(fn::class.java.classLoader.toWaveBeansClassLoader())
        return WrapFn(FnInitParameters().add(fnClazz, fn::class.jvmName))
    }

    override fun asString(fn: Fn<T, R>): String {
        val fnClazz = fn::class.className()
        val params = fn.initParams.params.map { "${it.key}:${it.value}" }.joinToString(";")
        return "$fnClazz|$params"
    }

    @Suppress("UNCHECKED_CAST")
    override fun fromString(s: String): Fn<T, R> {
        val (fnClazzStr, paramsStr) = s.split("|").take(2)
        val fnClazz = Class.forName(fnClazzStr) as Class<Fn<T, R>>
        val params = paramsStr.split(";")
            .filter { it.isNotBlank() }
            .associate {
                val (k, v) = it.split(":", limit = 2)
                k to if (v == "null") {
                    null
                } else {
                    v
                }
            }
        return instantiate(fnClazz.kotlin, FnInitParameters(params))
    }

    @Suppress("UNCHECKED_CAST")
    override fun instantiate(
        clazz: KClass<out Fn<T, R>>,
        initParams: FnInitParameters
    ): Fn<T, R> {
        val jClazz = clazz.java
        return jClazz.declaredConstructors
            .firstOrNull { with(it.parameterTypes) { size == 1 && get(0).isAssignableFrom(FnInitParameters::class.java) } }
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
                "$clazz has no proper constructor with ${FnInitParameters::class} as only one parameter or empty at all, " +
                        "it has: ${jClazz.declaredConstructors.joinToString { it.parameterTypes.toList().toString() }}"
            )
    }

}

/**
 * Helper [Fn] to wrap lambda functions within [Fn] instance to provide more friendly API.
 */
@Suppress("UNCHECKED_CAST")
internal class WrapFn<T, R>(initParams: FnInitParameters) : Fn<T, R>(initParams) {

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