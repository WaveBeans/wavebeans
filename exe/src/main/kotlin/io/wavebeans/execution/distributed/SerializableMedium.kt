package io.wavebeans.execution.distributed

import io.wavebeans.execution.medium.Medium
import io.wavebeans.execution.medium.MediumBuilder
import io.wavebeans.execution.medium.MediumSerializer

class SerializableMediumBuilder : MediumBuilder {
    override fun from(objects: List<Any>): Medium = SerializableMedium(objects)
}

/**
 * [Medium] for distributed execution.
 */
class SerializableMedium(val items: List<Any>) : Medium {

    override fun serializer(): MediumSerializer = TODO()

    override fun extractElement(at: Int): Any? = TODO()
}