package io.wavebeans.lib.io

import io.wavebeans.lib.io.BlockFormat.mdat
import io.wavebeans.lib.io.BlockFormat.unknown
import java.io.DataInputStream
import java.io.File

class M4ADecoder {
}

enum class BlockFormat(val int: Int) {
    unknown(0),
    ftyp(0x66747970),
    mdat(0x6D646174),
    moov(0x6D6F6F76),
    free(0x66726565)
    ;

    companion object {
        fun decode(v: Int): String {
            return (3 downTo 0).map { ((v ushr it * 8) and 0xFF) }
                .map { it.toChar() }
                .joinToString("")
        }

        fun of(int: Int): BlockFormat? {
            return values().firstOrNull { it.int == int }
        }
    }
}

// https://github.com/ahyattdev/M4ATools/blob/master/Sources/M4ATools/M4AFile.swift
// https://b.goeswhere.com/ISO_IEC_14496-12_2015.pdf
// http://www.telemidia.puc-rio.br/~rafaeldiniz/public_files/normas/ISO-14496/ISO_IEC_14496-1_2004(E).pdf
// https://mp4ra.org/
fun main(args: Array<String>) {
    val f = File("/users/asubb/Downloads/Untitled.m4a")
    val blocks = ArrayList<Block>()
    DataInputStream(f.inputStream().buffered()).use {
        do {
            val size = it.readBEInt() ?: break
            val type = it.readType() ?: break

            if (size == 1 && type == mdat) {
                TODO("handle large size")
            }

            val buf = ByteArray(size - 8)
            val readBytes = it.read(buf)
            if (readBytes > 0) {
                blocks += Block(type, buf)
            }
            println(
                "size=$size type=$type buf=\n${toHexString(buf)}"
            )
        } while (readBytes > 0)

    }
}

private fun toHexString(buf: ByteArray): String = buf.mapIndexed { idx, byte ->
    (byte.toInt() and 0xFF).let {
        if (it in 0x30..0x7f)
            it.toChar().toString().padStart(2, ' ')
        else it.toString(16).padStart(2, '0')
    } + if ((idx + 1) % 16 == 0) "\n" else " "
}.joinToString("")

fun DataInputStream.readType(): BlockFormat? {
    if (available() < 4) return null
    val typeData = readInt()
    val type = BlockFormat.of(typeData)
    return if (type == null) {
        println("Type 0x${typeData.toString(16)} (${BlockFormat.decode(typeData)}) is not recognized")
        unknown
    } else {
        type
    }
}

fun DataInputStream.readBEInt(): Int? {
    if (available() < 4) return null
    var acc = 0
    for (i in 0..3) {
        val readByte = read()
        require(readByte >= 0) { "no more bytes available" }
        acc = acc or (readByte shl (3 - i) * 8)
    }
    return acc
}

class Block(val type: BlockFormat, val buf: ByteArray) {

}