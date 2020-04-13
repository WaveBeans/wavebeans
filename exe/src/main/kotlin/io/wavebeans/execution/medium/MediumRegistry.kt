package io.wavebeans.execution.medium

import kotlin.reflect.KClass
import kotlin.reflect.KType

object MediumRegistry {

    fun register(clazz: KClass<*>, mediumCreator: (Any) -> Medium) {
        TODO()
    }

    fun creatorFor(clazz: KClass<*>): (Any) -> Medium {
        TODO()
    }
}