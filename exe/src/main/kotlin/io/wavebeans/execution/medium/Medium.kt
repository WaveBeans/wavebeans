package io.wavebeans.execution.medium

interface MediumSerializer {
    fun size(): Int

    fun writeTo(buf: ByteArray, at: Int)

    fun type(): String
}

interface Medium {
    fun serializer(): MediumSerializer

    fun extractElement(at: Int): Any?
}

interface MediumBuilder {
    fun from(objects: List<Any>): Medium
}

class PlainMediumBuilder : MediumBuilder {
    override fun from(objects: List<Any>): Medium = PlainMedium(objects)
}

//
//@Suppress("UNCHECKED_CAST")
//class MediumConverter {
//
//    fun listToMedium(objects: List<Any>): Medium {
//        require(objects.isNotEmpty()) { "Can't create medium around empty list" }
//        return when (val e1 = objects.first()) {
//            is Sample -> SampleMedium.create(objects as List<Sample>)
//            is FftSample -> {
//                FftSampleArrayMedium.create(objects as List<FftSample>)
//            }
//            is Window<*> -> {
//                when (val e2 = e1.elements.firstOrNull()) {
//                    is Sample -> WindowSampleMedium.create(objects as List<Window<Sample>>)
//                    else -> throw UnsupportedOperationException("${e2!!::class} is not supported")
//                }
//            }
//            is List<*> -> ListMedium.create(objects)
//            else -> throw UnsupportedOperationException("${e1::class} is not supported")
//        }
//    }
//}