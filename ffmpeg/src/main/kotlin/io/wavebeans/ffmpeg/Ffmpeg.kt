package io.wavebeans.ffmpeg

import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.io.WavHeader
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3
import org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import org.bytedeco.ffmpeg.global.avcodec.avcodec_open2
import org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame
import org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet
import org.bytedeco.ffmpeg.global.avformat.av_dump_format
import org.bytedeco.ffmpeg.global.avformat.av_read_frame
import org.bytedeco.ffmpeg.global.avformat.avformat_close_input
import org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO
import org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import org.bytedeco.ffmpeg.global.avutil.av_get_bytes_per_sample
import org.bytedeco.ffmpeg.global.avutil.av_strerror
import org.bytedeco.javacpp.PointerPointer
import java.io.File

class Ffmpeg {
}

fun main() {
    val fmtCtx = AVFormatContext(null)
    val pkt = AVPacket()
    val file = "${System.getProperty("user.home")}/tmp/guitar.wav"
    avformat_open_input(fmtCtx, file, null, null).throwIfError()

    avformat_find_stream_info(fmtCtx, null as PointerPointer<*>?).throwIfError()

    av_dump_format(fmtCtx, 0, file, 0)

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
        wavFile.outputStream().use { fos ->
            fos.write(wav.header())
            while (av_read_frame(fmtCtx, pkt) >= 0) {
                if (pkt.stream_index() == streamIdx) {
                    avcodec_send_packet(codecCtx, pkt).throwIfError()
                    avcodec_receive_frame(codecCtx, frame).throwIfError()

                    val data = frame.data(0)
                    val dataSize = av_get_bytes_per_sample(codecCtx.sample_fmt())
                    println("dataSize=$dataSize")
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
        val buf = ByteArray(1024)
        val r = av_strerror(this, buf, 1024L)
        val end = buf.indexOfFirst { it.toInt() == 0 }.takeIf { it >= 0 }
        throw Exception(
            if (r >= 0) {
                buf.decodeToString(0, end ?: buf.size)
            } else {
                "unknown error $this"
            }
        )
    }
}