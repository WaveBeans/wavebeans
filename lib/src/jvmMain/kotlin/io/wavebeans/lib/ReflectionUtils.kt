package io.wavebeans.lib

import kotlin.reflect.KClass

actual fun KClass<*>.className(): String {
    return this.qualifiedName!!
}