package io.wavebeans.http.rtsp

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.rtsp.RtspMethods
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class RtspRecordReceiverHandler : ChannelInboundHandlerAdapter() {

    private val log = KotlinLogging.logger { }

    private val passthroughTokens = listOf(
            RtspMethods.DESCRIBE.name(),
            RtspMethods.OPTIONS.name(),
            RtspMethods.ANNOUNCE.name(),
            RtspMethods.RECORD.name(),
            RtspMethods.PAUSE.name(),
            RtspMethods.PLAY.name(),
            RtspMethods.TEARDOWN.name(),
            RtspMethods.GET_PARAMETER.name(),
            RtspMethods.REDIRECT.name(),
            RtspMethods.SET_PARAMETER.name(),
            RtspMethods.SETUP.name(),
    ).map { it.toByteArray() }

    private val passthroughTokenMaxLength = passthroughTokens.maxOf { it.size }
    private val channelStates = ConcurrentHashMap<String, RtspRecordReceiverChannelState>()

    fun registerSession(session: RtspRecordingSession) {
        log.debug { "Attempting register session $session" }
        require(
                channelStates.putIfAbsent(
                        session.nettyChannelId,
                        RtspRecordReceiverChannelState.create(session)
                ) == null
        ) {
            "Channel ${session.nettyChannelId} is already registered"
        }
    }

    fun unregisterSession(
            channelId: String
    ) {
        log.debug { "Attempting unregister session for channel $channelId" }
        channelStates.remove(channelId)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        log.trace { "Received $msg" }
        if (msg is ByteBuf) {
            val previewSize = min(passthroughTokenMaxLength, msg.readableBytes())
            val preview = ByteArray(previewSize)
            msg.copy(0, previewSize).readBytes(preview)
            if (passthroughTokens.any { it.contentEquals(preview.copyOfRange(0, it.size)) }) {
                log.trace { "Passing through $msg" }
                ctx.fireChannelRead(msg)
            } else {
                val channelId = ctx.channel().id().asLongText()
                log.trace { "Handling $msg as RTP for channelId=$channelId" }
                val state = checkNotNull(channelStates[channelId]) {
                    "Channel $channelId should have been registered by that moment"
                }
                state.session.accessed()
                val buffer = ByteArrayOutputStream()
                msg.readBytes(buffer, msg.readableBytes()) // read the rest
                msg.release()
                val bytes = buffer.toByteArray()
                ctx.fireChannelReadComplete()
                var i = 0
                while (i < bytes.size) {
                    when (state.currentState) {
                        0 -> {
                            if (bytes[i] == '$'.toByte()) state.currentState++
                        }
                        1 -> {
                            state.currentChannel = bytes[i].toInt() and 0xFF
                            state.currentState++
                        }
                        2 -> {
                            state.currentPacketSize = (bytes[i].toInt() and 0xFF) shl 8
                            state.currentState++
                        }
                        3 -> {
                            state.currentPacketSize = state.currentPacketSize or (bytes[i].toInt() and 0xFF)
                            state.currentState++
                            log.trace { "Located packet of channel=${state.currentChannel}, bytesInThePacket=${state.currentPacketSize}." }
                            state.currentBuffer = ByteArray(state.currentPacketSize)
                            state.bytesLeftToRead = state.currentPacketSize
                        }
                        4 -> {
                            if (state.bytesLeftToRead > 0) {
                                state.currentBuffer[state.currentPacketSize - state.bytesLeftToRead] = bytes[i]
                                state.bytesLeftToRead--
                            }
                            if (state.bytesLeftToRead == 0) {
                                log.trace { "Read the packet of channel=${state.currentChannel}, bytesInThePacket=${state.currentPacketSize}" }
                                // read RTP header: https://tools.ietf.org/html/rfc3550#section-5.1
                                val i1 = state.currentBuffer.take(4)
                                        .mapIndexed { j, b -> (b.toLong() and 0xFF) shl (8 * (3 - j)) }
                                        .reduce { acc, j -> acc or j }
                                val version = (i1 ushr 30) and 0x03
                                require(version == 2L) { "RTPHeader.version=$version. Version 2 is supported only." }
                                val padding = (i1 ushr 29) and 0x01
                                val extension = (i1 ushr 28) and 0x01
                                require(extension == 0L) { "RTPHeader.extension=$extension. Non-0 value is not implemented." }
                                val csrcCount = (i1 ushr 24) and 0x07
                                require(csrcCount == 0L) { "RTPHeader.csrcCount=$csrcCount. Non-0 is not implemented." }
                                val marker = (i1 ushr 23) and 0x01
//                                require(marker == 0L) { "RTPHeader.marker=$marker. Non-0 value is not implemented." }
                                val payload = (i1 ushr 16) and 0x7F
                                val sequenceNumber = i1 and 0xFFFF
                                val timestamp = state.currentBuffer.drop(4).take(4)
                                        .mapIndexed { j, b -> (b.toLong() and 0xFF) shl (8 * (3 - j)) }
                                        .reduce { acc, j -> acc or j }
                                val ssrc = state.currentBuffer.drop(8).take(4)
                                        .mapIndexed { j, b -> (b.toLong() and 0xFF) shl (8 * (3 - j)) }
                                        .reduce { acc, j -> acc or j }
                                val csrc = (0 until csrcCount).map {
                                    state.currentBuffer.drop(12 + 4 * i).take(4)
                                            .mapIndexed { j, b -> (b.toLong() and 0xFF) shl (8 * (3 - j)) }
                                            .reduce { acc, j -> acc or j }
                                }

                                log.trace {
                                    """
                                        RTP Header:
                                            version=$version
                                            padding=$padding
                                            extension=$extension
                                            csrcCount=$csrcCount
                                            marker=$marker
                                            payload=$payload
                                            sequenceNumber=$sequenceNumber
                                            timestamp=$timestamp
                                            ssrc=$ssrc
                                            csrc=$csrc
                                    """.trimIndent()
                                }

                                val rtpHeaderSize = 12 + csrcCount.toInt()

                                if (state.currentChannel == state.session.dataChannel) {
                                    state.session.bufferHandler.invoke(
                                            state.session.sessionId,
                                            state.session.channelMappings.getValue(state.currentChannel),
                                            state.currentBuffer.copyOfRange(rtpHeaderSize, state.currentBuffer.size)
                                    )
                                }
                                state.currentState = 0
                            }
                        }
                        else -> throw UnsupportedOperationException("state=${state.currentState}")
                    }
                    i++
                }
                log.trace { "Finished the buffer with state=${state.currentState}, bytesLeftToRead=${state.bytesLeftToRead}" }
            }
        } else {
            throw UnsupportedOperationException("$msg is unsupported")
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // Close the connection when an exception is raised.
        log.error(cause) { "Error in $ctx" }
        ctx.close()
    }
}