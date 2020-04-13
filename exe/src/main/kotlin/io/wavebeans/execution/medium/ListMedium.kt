package io.wavebeans.execution.medium

class ListMedium(
        val items: List<Any>
) : Medium {

    companion object {
        fun create(items: List<Any>) = ListMedium(items)
    }

    override fun serializer(): MediumSerializer {
        TODO()
//        val l = items.encodeBytesAndType()
//        return SerializedObj(l.first, l.second)
    }

    override fun extractElement(at: Int): Any? = if (at < items.size) items[at] else null
}

