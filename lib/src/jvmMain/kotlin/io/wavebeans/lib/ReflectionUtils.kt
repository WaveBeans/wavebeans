package io.wavebeans.lib

import kotlin.reflect.KClass

actual fun KClass<*>.className(): String {
    return requireNotNull(this.qualifiedName) { "$this doesn't define qualifiedName"}
}