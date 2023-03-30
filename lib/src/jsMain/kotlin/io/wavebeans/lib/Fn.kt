package io.wavebeans.lib

import kotlin.reflect.KClass

/**
 * Wraps lambda function [fn] to a proper [Fn] class using generic wrapper [WrapFn]. The different between using
 * that method and creating a proper class declaration is that this implementation doesn't allow to by pass parameters
 * as [initParams] is not available inside lambda function.
 *
 * ```kotlin
 * Fn.wrap { it.doSomethingAndReturn() }
 * ```
 */
actual fun <T, R> wrap(fn: (T) -> R): Fn<T, R> {
    TODO("Not yet implemented")
}

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
 * If you don't need to specify any parameters for the function execution, you may use [Fn.wrap] method to make the instance.
 * of function out of lamda function.
 */
actual abstract class Fn<T, R> actual constructor(initParams: FnInitParameters) {
    actual abstract fun apply(argument: T): R

    /**
     * Gets the compact representation the function as string.
     */
    actual fun asString(): String {
        TODO("Not yet implemented")
    }

    actual val initParams: FnInitParameters
        get() = TODO("Not yet implemented")

}

/**
 * Creates the instance based on the string generated by [asString].
 */
actual fun <T, R> fromString(value: String): Fn<T, R> {
    TODO("Not yet implemented")
}

/**
 * Helper [Fn] to wrap lambda functions within [Fn] instance to provide more friendly API.
 */
actual class WrapFn<T, R> actual constructor(initParams: FnInitParameters) : Fn<T, R>(initParams) {

    actual override fun apply(argument: T): R {
        TODO("Not yet implemented")
    }
}

actual fun <T, R> instantiate(
    clazz: KClass<out Fn<T, R>>,
    initParams: FnInitParameters
): Fn<T, R> {
    TODO("Not yet implemented")
}