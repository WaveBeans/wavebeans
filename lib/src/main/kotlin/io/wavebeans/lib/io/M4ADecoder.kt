package io.wavebeans.lib.io

import io.wavebeans.lib.io.BoxType.*
import java.io.DataInputStream
import java.io.File

class M4ADecoder {
}

enum class BoxType(val int: Int, val createBox: (Box, Int) -> Box) {
    root(0, { _, size -> RootBox() }),
    unknown(-1, { _, _ -> throw UnsupportedOperationException("unknown box type can't be created") }),
    ftyp(0x66747970, { parent, size -> FileTypeBox(parent, size) }),
    mdat(0x6D646174, { parent, size -> MediaDataBox(parent, size) }),
    moov(0x6D6F6F76, { parent, size -> MovieBox(parent, size) }),
    free(0x66726565, { parent, size -> FreeBox(parent, size) }),
    mvhd(0x6d766864, { parent, size -> MovieHeaderBox(parent, size) }),
    trak(0x7472616b, { parent, size -> TrackBox(parent, size) }),
    tkhd(0x746b6864, { parent, size -> TrackHeaderBox(parent, size) }),
    mdia(0x6d646961, { parent, size -> MediaBox(parent, size) }),
    mdhd(0x6d646864, { parent, size -> MediaHeaderBox(parent, size) }),
    hdlr(0x68646c72, { parent, size -> HandlerBox(parent, size) }),
    minf(0x6d696e66, { parent, size -> MediaInformationBox(parent, size) }),
    stbl(0x7374626c, { parent, size -> SampleTableBox(parent, size) }),
    stsd(0x73747364, { parent, size -> SampleDescriptionBox(parent, size) }),
    smhd(0x736d6864, { parent, size -> SoundMediaHeaderBox(parent, size) }),

    ;

    companion object {

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
//    val f = File("${System.getProperty("user.home")}/Downloads/Untitled.m4a")
    val f = File("${System.getProperty("user.home")}/tmp/a3b-tone-deprot.mp4")
    DataInputStream(f.inputStream().buffered()).use {
        RootBox().read(LimitingDataStream(it, Int.MAX_VALUE))
    }
}

abstract class Box(
    val parent: Box?,
    val size: Int,
    val type: BoxType
) {

    val children = ArrayList<Box>()

    var buf: ByteArray? = null
        protected set

    open fun read(inputStream: LimitingDataStream) {
        do {
            val size = inputStream.readBEInt()
            if (size == 0) {
                println(
                    "No more boxes found in $type, still available ${inputStream.available()} bytes, " +
                            "remaining on the stream is ${inputStream.remaining()}"
                )
                break
            }
            val type = inputStream.readType()

            if (size == 1 && type == mdat) {
                TODO("Handle large size content")
            }

            if (type != unknown) {
                println("---- Discovered box $type of size $size")
                val boxStream = inputStream.substream(size - 8)
                val box = type.createBox(this, size - 8)
                box.read(boxStream)
                println(">> Finished reading $type. Remaining=${boxStream.remaining()}")
                println("$box")
                box.buf?.toHexString()?.let(::println)
                println("---- end of box $type")

                children += box
            } else {
                val buf = inputStream.readBuffer(size - 8)
                println("Read $size bytes of type $type:\n${buf.toHexString()}")
            }
        } while (inputStream.remaining() > 0 && inputStream.available() > 0)
    }

    override fun toString(): String {
        return "|${this::class.simpleName}| $type box[size=$size]{${parent?.type ?: ""}}"
    }
}

abstract class FullBox(parent: Box, size: Int, type: BoxType) : Box(parent, size, type) {
    protected var version = -1
    protected var flags: IntArray = IntArray(0)

    override fun read(inputStream: LimitingDataStream) {
        version = inputStream.readUnsignedByte()
        flags = IntArray(3) { inputStream.readUnsignedByte() }
        readRest(inputStream)
    }

    open fun readRest(inputStream: LimitingDataStream) {
        buf = inputStream.readBuffer(inputStream.remaining())
    }

    override fun toString(): String {
        return "FullBox(version=$version, flags=${flags.contentToString()})"
    }

}

class RootBox() : Box(null, Int.MAX_VALUE, root)

class FileTypeBox(parent: Box, size: Int) : Box(parent, size, ftyp) {

    var majorBrand: Int = 0
    var minorVersion: Int = 0
    var compatibleBrands: IntArray = IntArray(0)

    override fun read(inputStream: LimitingDataStream) {
        var remain = size
        require(remain >= 8)
        majorBrand = requireNotNull(inputStream.readBEInt())
        remain -= 4
        minorVersion = requireNotNull(inputStream.readBEInt())
        remain -= 4
        require(remain % 4 == 0)
        compatibleBrands = IntArray(remain / 4) { requireNotNull(inputStream.readBEInt()) }
    }

    override fun toString(): String {
        return "FileTypeBox(majorBrand=0x${majorBrand.toString(16)} (${MacOsRoman.decode(majorBrand)}), " +
                "minorVersion=0x${minorVersion.toString(16)}, " +
                "compatibleBrands=${
                    compatibleBrands.joinToString { "0x${it.toString(16)} (${MacOsRoman.decode(it)})" }
                }, " +
                "size=$size, type=$type)"
    }


}

class MediaDataBox(parent: Box, size: Int) : Box(parent, size, mdat) {
    override fun read(inputStream: LimitingDataStream) {
        buf = inputStream.readBuffer(size)
    }
}

class MovieBox(parent: Box, size: Int) : Box(parent, size, moov)

class MovieHeaderBox(parent: Box, size: Int) : Box(parent, size, mvhd) {
    override fun read(inputStream: LimitingDataStream) {
        buf = inputStream.readBuffer(size)
    }
}

class TrackBox(parent: Box, size: Int) : Box(parent, size, trak)

class TrackHeaderBox(parent: Box, size: Int) : FullBox(parent, size, tkhd) {

    private var creationTime: Long = -1
    private var modificationTime: Long = -1
    private var trackId: Long = -1
    private var duration: Long = -1
    private var layer: Short = -1
    private var alternateGroup: Short = -1
    private var volume: Short = -1
    private var matrix: IntArray = IntArray(0)
    private var width: Int = -1
    private var height: Int = -1

    override fun readRest(inputStream: LimitingDataStream) {
        if (version == 1) {
            creationTime = inputStream.readLong()
            modificationTime = inputStream.readLong()
            trackId = inputStream.readUnsignedInt()
            inputStream.readInt() // reserved
            duration = inputStream.readLong()
        } else {
            creationTime = inputStream.readUnsignedInt()
            modificationTime = inputStream.readUnsignedInt()
            trackId = inputStream.readUnsignedInt()
            inputStream.readInt() // reserved
            duration = inputStream.readUnsignedInt()
        }
        inputStream.readInt() // reserved
        layer = inputStream.readShort()
        alternateGroup = inputStream.readShort()
        volume = inputStream.readShort() // TODO if track_is_audio 0x0100 else 0
        matrix = IntArray(9) { inputStream.readInt() }
        width = inputStream.readInt()
        height = inputStream.readInt()
        buf = inputStream.readBuffer(inputStream.remaining())
    }

    override fun toString(): String {
        return "TrackHeaderBox(version=$version, flags=${flags.contentToString()}, creationTime=$creationTime, " +
                "modificationTime=$modificationTime, trackId=$trackId, duration=$duration, layer=$layer, " +
                "alternateGroup=$alternateGroup, volume=$volume, matrix=${matrix.contentToString()}, " +
                "width=$width, height=$height, size=$size, $type=$type)"
    }

}

class MediaBox(parent: Box, size: Int) : Box(parent, size, mdia)

class MediaHeaderBox(parent: Box, size: Int) : FullBox(parent, size, mdhd) {

    private var creationTime = -1L
    private var modificationTme = -1L
    private var timescale = -1L
    private var duration = -1L
    private var pad = false
    private var language = ""
    private var preDefined = -1

    override fun readRest(inputStream: LimitingDataStream) {
        if (version == 1) {
            creationTime = inputStream.readLong()
            modificationTme = inputStream.readLong()
            timescale = inputStream.readUnsignedInt()
            duration = inputStream.readLong()
        } else {
            creationTime = inputStream.readUnsignedInt()
            modificationTme = inputStream.readUnsignedInt()
            timescale = inputStream.readUnsignedInt()
            duration = inputStream.readUnsignedInt()
        }
        inputStream.readShort().toInt().also {
            pad = it shr 15 == 1
            language = (0..2).asSequence()
                .map { i -> ((it shr (5 * i) and 0x1F) + 0x60).toChar() }
                .joinToString(separator = "")
        }
        preDefined = inputStream.readShort().toInt()
    }

    override fun toString(): String {
        return "MediaHeaderBox(creationTime=$creationTime, modificationTme=$modificationTme, timescale=$timescale, " +
                "duration=$duration, pad=$pad, language=$language, preDefined=$preDefined), version=$version, " +
                "flags=${flags.contentToString()}"
    }


}

class HandlerBox(parent: Box, size: Int) : FullBox(parent, size, hdlr) {
    private var preDefined = -1L
    private var handlerType = -1L
    private var name = ""

    override fun readRest(inputStream: LimitingDataStream) {
        preDefined = inputStream.readUnsignedInt()
        handlerType = inputStream.readUnsignedInt()
        name = inputStream.readBuffer(inputStream.remaining()).decodeToString()
    }

    override fun toString(): String {
        return "HandlerBox(preDefined=$preDefined, handlerType=0x${handlerType.toString(16)}(${
            MacOsRoman.decode(
                handlerType.toInt()
            )
        }), name='$name')"
    }

}

class MediaInformationBox(parent: Box, size: Int) : Box(parent, size, minf)

class SampleTableBox(parent: Box, size: Int) : Box(parent, size, stbl)

abstract class SampleEntry(parent: Box, size: Int, type: BoxType) : Box(parent, size, type) {

    protected var dataReferenceIndex = -1
    protected var format = -1

    override fun read(inputStream: LimitingDataStream) {
        format = inputStream.readInt()
        inputStream.readBuffer(6) // reserved
        dataReferenceIndex = inputStream.readUnsignedShort()
        readRest(inputStream)
    }

    open fun readRest(inputStream: LimitingDataStream) {
        buf = inputStream.readBuffer(inputStream.remaining())
    }

    override fun toString(): String {
        return "SampleEntry(dataReferenceIndex=$dataReferenceIndex)"
    }

}

class AudioSampleEntry(parent: Box, size: Int) : SampleEntry(parent, size, unknown) {
    private var channelCount = -1
    private var sampleSize = -1
    private var sampleRate = -1

    override fun readRest(inputStream: LimitingDataStream) {
        inputStream.readBuffer(8) // reserved
        channelCount = inputStream.readUnsignedShort()
        sampleSize = inputStream.readUnsignedShort()
        inputStream.readBuffer(2) // preDefined
        sampleRate = inputStream.readInt()

        buf = inputStream.readBuffer(inputStream.remaining())
    }

    override fun toString(): String {
        return "AudioSampleEntry(format=0x${format.toString(16)}(${MacOsRoman.decode(format)}), " +
                "channelCount=$channelCount, sampleSize=$sampleSize, sampleRate=$sampleRate, " +
                "dataReferenceIndex=$dataReferenceIndex)"
    }
}

class SampleDescriptionBox(parent: Box, size: Int) : FullBox(parent, size, stsd) {
    private val sampleEntries = ArrayList<SampleEntry>()
    override fun readRest(inputStream: LimitingDataStream) {
        require(parent?.parent?.children?.any { it is SoundMediaHeaderBox } != null) { "Only audio entries are supported now" }
        require(version == 0) { "Version 1 is yet unsupported" }
        val entryCount = inputStream.readInt();
        repeat(entryCount) { idx ->
            val size = inputStream.readInt() - 4
            println("========= entry #${idx + 1}")
            val sampleEntry = AudioSampleEntry(this, size)
            sampleEntry.read(inputStream.substream(size))
            println(sampleEntry)
            sampleEntry.buf?.also { println(it.toHexString()) }
            sampleEntries += sampleEntry
            println("=== end of entry #${idx + 1}")
        }
    }

    override fun toString(): String {
        return "SampleDescriptionBox(sampleEntries=$sampleEntries, version=$version, flags=${flags.contentToString()})"
    }

}

class SoundMediaHeaderBox(parent: Box, size: Int) : FullBox(parent, size, smhd) {
    private var balance = 0.0f

    override fun readRest(inputStream: LimitingDataStream) {
        balance = inputStream.readShort().toFloat() / Short.MAX_VALUE
        inputStream.readShort() // reserved
    }

    override fun toString(): String {
        return "SoundMediaHeaderBox(balance=$balance, version=$version, flags=${flags.contentToString()})"
    }
}

class FreeBox(parent: Box, size: Int) : Box(parent, size, free) {
    override fun read(inputStream: LimitingDataStream) {
        // discard the bytes
        inputStream.readBuffer(size)
    }
}

private fun ByteArray.toHexString(): String = mapIndexed { idx, byte ->
    (byte.toInt() and 0xFF).let {
        if (it in 0x30..0x7f)
            it.toChar().toString().padStart(2, ' ')
        else it.toString(16).padStart(2, '0')
    } + if ((idx + 1) % 16 == 0) "\n" else " "
}.joinToString("")

class LimitingDataStream(private val stream: DataInputStream, limit: Int) {

    private var remaining = limit

    fun substream(size: Int): LimitingDataStream {
        reserve(size)
        return LimitingDataStream(stream, size)
    }

    fun readInt(): Int {
        reserve(4)
        return stream.readInt()
    }

    fun readUnsignedByte(): Int {
        reserve(1)
        return stream.readUnsignedByte()
    }

    fun readShort(): Short {
        reserve(2)
        return stream.readShort()
    }

    fun readUnsignedShort(): Int {
        reserve(2)
        var acc = 0
        for (i in 0..1) {
            val readByte = stream.readUnsignedByte().toInt()
            require(readByte >= 0) { "no more bytes available" }
            acc = acc or (readByte shl (1 - i) * 8)
        }
        return acc
    }

    fun readLong(): Long {
        reserve(8)
        return stream.readLong()
    }

    fun readBuffer(size: Int): ByteArray {
        reserve(size)
        val buf = ByteArray(size)
        val bytesRead = stream.read(buf)
        require(bytesRead == size) { "Expected to read $size bytes but read $bytesRead" }
        return buf

    }

    fun readType(): BoxType {
        reserve(4)
        val typeData = stream.readInt()
        val type = BoxType.of(typeData)
        return if (type == null) {
            println("Type 0x${typeData.toString(16)} (${MacOsRoman.decode(typeData)}) is not recognized")
            unknown
        } else {
            type
        }
    }

    fun readUnsignedInt(): Long {
        reserve(4)
        var acc = 0L
        for (i in 0..3) {
            val readByte = stream.readUnsignedByte().toLong()
            require(readByte >= 0) { "no more bytes available" }
            acc = acc or (readByte shl (3 - i) * 8)
        }
        return acc
    }

    fun readBEInt(): Int {
        reserve(4)
        var acc = 0
        for (i in 0..3) {
            val readByte = stream.readUnsignedByte()
            require(readByte >= 0) { "no more bytes available" }
            acc = acc or (readByte shl (3 - i) * 8)
        }
        return acc
    }

    fun readBoolean(): Boolean {
        reserve(1)
        return stream.readByte().toInt() and 0xFF != 1
    }

    fun remaining(): Int {
        return remaining
    }

    fun available(): Int {
        return stream.available();
    }

    private fun reserve(size: Int) {
        require(remaining >= size) { "Should be more than $size bytes but only $remaining bytes remaining " }
        require(available() >= size) { "The stream has only ${available()} bytes but requested to read $size bytes" }
        remaining -= size
    }


}

object MacOsRoman {
    fun decode(v: Int): String {
        return (3 downTo 0).map { ((v ushr it * 8) and 0xFF) }
            .map { it.toChar() }
            .joinToString("")
    }

}