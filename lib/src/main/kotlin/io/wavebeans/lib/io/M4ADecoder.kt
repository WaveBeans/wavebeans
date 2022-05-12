package io.wavebeans.lib.io

import io.wavebeans.lib.io.BoxType.*
import java.io.DataInputStream
import java.io.File

class M4ADecoder {
}

enum class BoxType(val int: Int, val createBox: (Int, DataInputStream) -> Box) {
    root(0, { size, stream -> RootBox(stream) }),
    unknown(
        -1,
        { _, _ -> throw kotlin.UnsupportedOperationException("unknown box type can't be created") }),
    ftyp(0x66747970, { size, stream -> FtypeBox(size, stream) }),
    mdat(0x6D646174, { size, stream -> MdatBox(size, stream) }),
    moov(0x6D6F6F76, { size, stream -> MoovBox(size, stream) }),
    free(0x66726565, { size, stream -> FreeBox(size, stream) })
    ;

    companion object {
        fun decode(v: Int): String {
            return (3 downTo 0).map { ((v ushr it * 8) and 0xFF) }
                .map { it.toChar() }
                .joinToString("")
        }

        fun of(int: Int): BoxType? {
            return values().firstOrNull { it.int == int }
        }
    }
}

// https://github.com/ahyattdev/M4ATools/blob/master/Sources/M4ATools/M4AFile.swift
// https://b.goeswhere.com/ISO_IEC_14496-12_2015.pdf
// http://www.telemidia.puc-rio.br/~rafaeldiniz/public_files/normas/ISO-14496/ISO_IEC_14496-1_2004(E).pdf
// https://mp4ra.org/
fun main(args: Array<String>) {
    val f = File("${System.getProperty("user.home")}/Downloads/Untitled.m4a")
    DataInputStream(f.inputStream().buffered()).use {
        RootBox(it).read()
    }
}

abstract class Box(val size: Int, val type: BoxType, val inputStream: DataInputStream) {
    abstract fun read()
}

class RootBox(inputStream: DataInputStream) : Box(0, root, inputStream) {

    val children = ArrayList<Box>()

    override fun read() {
        do {
            val size = inputStream.readBEInt() ?: break
            val type = inputStream.readType() ?: break

            if (size == 1 && type == mdat) {
                TODO("Handle large size content")
            }

            if (type != unknown) {
                children += type.createBox(size - 8, inputStream).also { it.read() }
            } else {
                val buf = inputStream.readBuffer(size - 8)
                println("Read $size bytes of type $type:\n${buf.toHexString()}")
            }
        } while (inputStream.available() > 0)
    }
}

class FtypeBox(size: Int, stream: DataInputStream) : Box(size, ftyp, stream) {
    override fun read() {
        val buf = inputStream.readBuffer(size)
        println("Read $size bytes of type $type:\n${buf.toHexString()}")
    }
}

class MdatBox(size: Int, stream: DataInputStream) : Box(size, mdat, stream) {
    override fun read() {
        val buf = inputStream.readBuffer(size)
        println("Read $size bytes of type $type:\n${buf.toHexString()}")
    }
}

class MoovBox(size: Int, stream: DataInputStream) : Box(size, moov, stream) {
    override fun read() {
        val buf = inputStream.readBuffer(size)
        println("Read $size bytes of type $type:\n${buf.toHexString()}")
    }
}

class FreeBox(size: Int, stream: DataInputStream) : Box(size, free, stream) {
    override fun read() {
        val buf = inputStream.readBuffer(size)
        println("Read $size bytes of type $type:\n${buf.toHexString()}")
    }
}

private fun ByteArray.toHexString(): String = mapIndexed { idx, byte ->
    (byte.toInt() and 0xFF).let {
        if (it in 0x30..0x7f)
            it.toChar().toString().padStart(2, ' ')
        else it.toString(16).padStart(2, '0')
    } + if ((idx + 1) % 16 == 0) "\n" else " "
}.joinToString("")

private fun DataInputStream.readBuffer(size: Int): ByteArray {
    val buf = ByteArray(size)
    val bytesRead = this.read(buf)
    require(bytesRead == size) { "Expected to read $size bytes but read $bytesRead" }
    return buf
}

private fun DataInputStream.readType(): BoxType? {
    if (available() < 4) return null
    val typeData = readInt()
    val type = BoxType.of(typeData)
    return if (type == null) {
        println("Type 0x${typeData.toString(16)} (${BoxType.decode(typeData)}) is not recognized")
        unknown
    } else {
        type
    }
}

private fun DataInputStream.readBEInt(): Int? {
    if (available() < 4) return null
    var acc = 0
    for (i in 0..3) {
        val readByte = read()
        require(readByte >= 0) { "no more bytes available" }
        acc = acc or (readByte shl (3 - i) * 8)
    }
    return acc
}

class IBlock(val type: BoxType, val buf: ByteArray) {

}