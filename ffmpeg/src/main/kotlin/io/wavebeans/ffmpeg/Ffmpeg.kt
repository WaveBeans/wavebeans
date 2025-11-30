package io.wavebeans.ffmpeg

import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.io.WavHeader
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVProbeData
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3
import org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import org.bytedeco.ffmpeg.global.avcodec.avcodec_open2
import org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame
import org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet
import org.bytedeco.ffmpeg.global.avformat.AVSEEK_SIZE
import org.bytedeco.ffmpeg.global.avformat.av_probe_input_format
import org.bytedeco.ffmpeg.global.avformat.av_read_frame
import org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avformat_close_input
import org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avformat.avio_alloc_context
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AVERROR_UNKNOWN
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO
import org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import org.bytedeco.ffmpeg.global.avutil.av_get_bytes_per_sample
import org.bytedeco.ffmpeg.global.avutil.av_malloc
import org.bytedeco.ffmpeg.global.avutil.av_strerror
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import java.io.File
import java.nio.ByteBuffer

class Ffmpeg {
}


const val SEEK_SET = 0
const val SEEK_CUR = 1
const val SEEK_END = 2
fun main() {
    val file = "${System.getProperty("user.home")}/tmp/guitar.wav"

    val buffer = ByteBuffer.wrap(File(file).readBytes())

    println("buffer=$buffer")
    val inputStream = BytePointer(buffer)
    val bufferSize = 512
    val ioContext = avio_alloc_context(
        BytePointer(av_malloc(bufferSize.toLong())),
        bufferSize,
        0,
        inputStream,
        object : Read_packet_Pointer_BytePointer_int() {
            override fun call(opaque: Pointer, buf: BytePointer, buf_size: Int): Int {
                try {
//                    println("read(opaque=$opaque, buf=${buf}, buf_size=$buf_size) buffer=$buffer")
                    val length = minOf(buffer.remaining(), buf_size/*, buf.capacity().toInt()*/)
                    if (length <= 0) {
                        return AVERROR_EOF
                    }
                    repeat(length) {
                        val b = buffer.get()
                        buf.put(it.toLong(), b)
                    }
//                    println("Read $length bytes")
                    return length
                } catch (e: Exception) {
                    e.printStackTrace()
                    return AVERROR_UNKNOWN
                }
            }
        },
        null,
        object : Seek_Pointer_long_int() {
            override fun call(opaque: Pointer, offset: Long, whence: Int): Long {
//                println("seek(opaque=$opaque, offset=$offset, whence=$whence")
                try {
                    if (whence == AVSEEK_SIZE)
                        return buffer.capacity().toLong()
                    when (whence) {
                        SEEK_SET -> buffer.position(offset.toInt())
                        SEEK_CUR -> buffer.position(buffer.position() + offset.toInt())
                        SEEK_END -> buffer.position(buffer.limit())
                        else -> throw IllegalArgumentException("Whence $whence is not recognized")
                    }
//                    println("Set to ${buffer.position()} position:$buffer")
                    return buffer.position().toLong()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return -1
                }
            }
        }
    )

    val fmtCtx = avformat_alloc_context()
    fmtCtx.pb(ioContext)
//    val fmtCtx = AVFormatContext(null)
//    avformat_open_input(fmtCtx, file, null, null).throwIfError()
//    av_dump_format(fmtCtx, 0, file, 0)

    val probeData = AVProbeData()
    probeData.buf(BytePointer(buffer))
    probeData.buf_size(1234)
    probeData.filename(BytePointer(""))
    val format = av_probe_input_format(probeData, 1)
    println(">>>> format=${format.long_name().stringBytes.decodeToString()}")

    fmtCtx.iformat(format)
    fmtCtx.flags(128) // AVFMT_FLAG_CUSTOM_IO

    buffer.rewind()
    avformat_open_input(fmtCtx, "", format, null).throwIfError()

    println("streams ${fmtCtx.streams()}")
    println("nb_stream=${fmtCtx.nb_streams()}")

    avformat_find_stream_info(fmtCtx, null as PointerPointer<*>?).throwIfError()
    var streamIdx: Int = -1
    for (it in 0 until fmtCtx.nb_streams()) {
        val codecType = fmtCtx.streams(it).codecpar().codec_type()
        if (codecType == AVMEDIA_TYPE_AUDIO) {
            streamIdx = it
            break
        }
    }
    require(streamIdx >= 0) { "audio stream is not found" }
    println("Picked stream #$streamIdx")
    fmtCtx.streams(streamIdx).codecpar().use {
        println("Sample rate: ${it.sample_rate()}")
        println("Bit depth: ${it.bits_per_coded_sample()}")
    }

    val codecCtx = avcodec_alloc_context3(null)
    avcodec_parameters_to_context(codecCtx, fmtCtx.streams(streamIdx).codecpar())
    val codec = avcodec_find_decoder(codecCtx.codec_id())
    requireNotNull(codec) { "Unsupported codec $codecCtx" }
    avcodec_open2(codecCtx, codec, null as PointerPointer<*>?).throwIfError()
    println("Context=$codecCtx codec=$codec")

    av_frame_alloc().use { frame ->
        val wav = WavHeader(BitDepth.BIT_32, 44100.0f, 1, 0x1FFFFFFF);
        val wavFile = File(File(file).parentFile, "output.wav")
        wavFile.createNewFile()
        wavFile.outputStream().buffered().use { fos ->
            fos.write(wav.header())
            val pkt = AVPacket()
            val dataSize = av_get_bytes_per_sample(codecCtx.sample_fmt())
            while (av_read_frame(fmtCtx, pkt) >= 0) {
                if (pkt.stream_index() == streamIdx) {
                    avcodec_send_packet(codecCtx, pkt).throwIfError()
                    avcodec_receive_frame(codecCtx, frame).throwIfError()

                    val data = frame.data(0)
                    val buf = ByteArray(dataSize * frame.nb_samples())
                    data.get(buf)
                    fos.write(buf)

                }
                av_packet_unref(pkt)
            }
        }
    }

    avformat_close_input(fmtCtx)
}

fun Int.throwIfError() {
    if (this < 0) {
        println("ERROR")
        val buf = ByteArray(1024)
        val r = av_strerror(this, buf, 1024L)
        val end = buf.indexOfFirst { it.toInt() == 0 }.takeIf { it >= 0 }
        throw IllegalStateException(
            if (r >= 0) {
                buf.decodeToString(0, end ?: buf.size)
            } else {
                "unknown error $this"
            }
        )
    }
}