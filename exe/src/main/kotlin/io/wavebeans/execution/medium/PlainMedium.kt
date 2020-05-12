package io.wavebeans.execution.medium

class PlainMediumBuilder : MediumBuilder {
    override fun from(objects: List<Any>): Medium = PlainMedium(objects)
}

/**
 * [Medium] for multi-threaded execution. It just keeps the objects intact for transferring over Pod lines.
 */
class PlainMedium(val items: List<Any>) : Medium {

    override fun extractElement(at: Int): Any? {
        return if (at < items.size) items[at] else null
    }
}