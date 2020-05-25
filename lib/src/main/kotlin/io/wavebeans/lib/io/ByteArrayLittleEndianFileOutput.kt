package io.wavebeans.lib.io

import io.wavebeans.lib.*

fun writeSampleAsLEBytes(buf: ByteArray, at: Int, element: Sample, bitDepth: BitDepth) {
    require(buf.size - at >= bitDepth.bytesPerSample) { "At least ${bitDepth.bytesPerSample} required but ${buf.size - at} available" }
    if (bitDepth != BitDepth.BIT_8) {
        val bytesAsLong = when (bitDepth) {
            BitDepth.BIT_16 -> element.asShort().toLong()
            BitDepth.BIT_24 -> element.as24BitInt().toLong()
            BitDepth.BIT_32 -> element.asInt().toLong()
            BitDepth.BIT_64 -> element.asLong()
            BitDepth.BIT_8 -> throw UnsupportedOperationException("Unreachable branch")
        }

        (0 until bitDepth.bytesPerSample)
                .map { j -> ((bytesAsLong shr j * 8) and 0xFF).toByte() }
                .forEachIndexed { j: Int, v: Byte ->
                    buf[at + j] = (v.toInt() and 0xFF).toByte()
                }
    } else {
        // 8 bit is kept as unsigned. https://en.wikipedia.org/wiki/WAV#Description
        // Follow this for explanation why it's done this way
        // https://en.wikipedia.org/wiki/Two's_complement#Converting_from_two.27s-complement_representation
        val asUByte = element.asByte().asUnsignedByte()
        buf[at] = asUByte.toByte()
    }

}