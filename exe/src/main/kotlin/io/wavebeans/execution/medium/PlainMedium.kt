package io.wavebeans.execution.medium

/**
 * [Medium] for multi-threaded execution. It just keeps the objects intact for transferring over Pod lines.
 */
class PlainMedium(val items: List<Any>) : Medium {

    override fun serializer(): MediumSerializer {
        throw UnsupportedOperationException("That medium only for multi-threaded execution. It can't serialize")
    }

    override fun extractElement(at: Int): Any? {
        return if (at < items.size) items[at] else null
    }
}