package io.wavebeans.http.rtsp

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.rtsp.*
import io.wavebeans.http.WbNettyHandler
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@ChannelHandler.Sharable
class RtpsRecordControllerHandler(
    val bufferHandler: (Long, RtpMapping, ByteArray) -> Unit,
    val tearDownHandler: (Long, RtpMapping) -> Unit,
    val sessionTtl: Long = 60_000L
) : ChannelInboundHandlerAdapter(), WbNettyHandler {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val channels = ConcurrentHashMap<String, RtspControllerChannelState>()
    private val sessions = ConcurrentHashMap<Long, RtspRecordingSession>()
    private val rtspRecordReceiverHandler = RtspRecordReceiverHandler()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    override fun init() {
        scheduler.scheduleAtFixedRate({
            sessions.forEach { (sessionId, session) ->
                if (session.isTooOld(sessionTtl)) {
                    doTearDown(sessionId)
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    override fun initChannel(channel: SocketChannel) {
        channel.pipeline().addLast(rtspRecordReceiverHandler)
            .addLast(RtspDecoder())
            .addLast(HttpObjectAggregator(4 * 1024))
            .addLast(RtspEncoder())
            .addLast(this)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val channelId = ctx.channel().id().asLongText()
        channels[channelId]?.let { it.accessedLastTime = System.currentTimeMillis() }
        log.trace { "Received $msg over channelId=$channelId" }
        if (msg is FullHttpRequest && msg.protocolVersion() == RtspVersions.RTSP_1_0) {
            log.trace { "Handling method ${msg.method()} on ${msg.uri()}" }
            val response = DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK)
            msg.headers()[RtspHeaderNames.CSEQ]?.let { response.headers().add(RtspHeaderNames.CSEQ, it) }
            val sessionId = msg.headers()[RtspHeaderNames.SESSION]
            when (msg.method()) {
                RtspMethods.OPTIONS -> {
                    response.headers().add(
                        RtspHeaderNames.PUBLIC,
                        listOf(
                            RtspMethods.OPTIONS,
                            RtspMethods.ANNOUNCE,
                            RtspMethods.SETUP,
                            RtspMethods.RECORD,
                            RtspMethods.TEARDOWN,
                        ).joinToString(", ")
                    )
                }
                RtspMethods.ANNOUNCE -> {
                    channels.computeIfAbsent(channelId) {
                        log.info { "New channel $channelId registered" }
                        RtspControllerChannelState.create()
                    }
                    val msgContent = msg.content()
                    handleAnnounce(msgContent, channelId, msg.uri())
                }
                RtspMethods.SETUP -> {
                    val transport = checkNotNull(msg.headers()[RtspHeaderNames.TRANSPORT]) { "Header ${RtspHeaderNames.TRANSPORT} is not found but required" }
                    val sid = doSetup(channelId, transport)
                    response.headers().add(RtspHeaderNames.TRANSPORT, transport)
                    response.headers().add(RtspHeaderNames.SESSION, sid)
                }
                RtspMethods.RECORD -> {
                    val sid = checkNotNull(sessionId) { "Header `${RtspHeaderNames.SESSION}` is not found but required." }.toLong()
                    doRecord(sid)
                }
                RtspMethods.TEARDOWN -> {
                    val sid = checkNotNull(sessionId) { "Header `${RtspHeaderNames.SESSION}` is not found but required." }.toLong()
                    doTearDown(sid)
                }
                else -> throw UnsupportedOperationException()
            }
            log.trace { "Responding $response" }
            ctx.write(response)
            ctx.flush()
        }
    }

    override fun close() {
        log.info { "Session clean up scheduler shutting down" }
        scheduler.shutdown()
        if (!scheduler.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            scheduler.shutdown()
        }
        log.info { "Session clean up scheduler has shut down" }
    }

    private fun doRecord(sessionId: Long) {
        val session = sessions.getValue(sessionId)
        session.accessed()
        log.debug { "[sid=$sessionId] Started record" }
        rtspRecordReceiverHandler.registerSession(session)
    }

    private fun doSetup(channelId: String, transport: String): Long {
        val sessionId = Random.nextLong(Long.MAX_VALUE)

        val values = transport.split(";")
        if (values[0] != "RTP/AVP/TCP") throw UnsupportedOperationException("Only TCP is supported")
        if (values[1] != "unicast") throw UnsupportedOperationException("Only `unicast` is supported")
        val mode = values.firstOrNull { it.startsWith("mode") }
            ?.split("=", limit = 2)
            ?.get(1)
        if (mode?.toLowerCase() != "record") throw UnsupportedOperationException("mode=$mode is not supported")

        val interleaved = values.firstOrNull { it.startsWith("interleaved") }
            ?.split("=", limit = 2)
            ?.get(1)
            ?: throw UnsupportedOperationException("interleaved must be specified")
        val (data, manage) = interleaved.split("-", limit = 2).map { it.toInt() }

        val channelState = channels.getValue(channelId)
        val channelMappings = channelState.announcements.map {
            require(it.fmtList.size == 1) { "fmtList != 1 is not supported" }
            val fmt = it.fmtList.first()
            require(fmt >= 96) { "Built in formats are not supported, only custom >= 96" }
            val rtmMap = checkNotNull(channelState.rtpMappings.firstOrNull { it.payloadType == fmt }) {
                "Format $fmt is not found among mappings: ${channelState.rtpMappings}"
            }
            it.port to rtmMap
        }.toMap()

        val session = RtspRecordingSession.create(channelId, sessionId, data, manage, channelMappings, bufferHandler, tearDownHandler)
        require(sessions.putIfAbsent(sessionId, session) == null) {
            "Can't create session with id $sessionId as it already exists o_O"
        }
        channels.remove(channelId) // no longer needed channel state -- proper session started
        return sessionId
    }

    private fun handleAnnounce(msgContent: ByteBuf, channelId: String, uri: String) {
        val buffer = ByteArrayOutputStream()
        msgContent.readBytes(buffer, msgContent.readableBytes())
        val content = String(buffer.toByteArray())
            .split("[\r\n]+".toRegex())
            .filterNot(String::isEmpty)
        log.debug { "[channelId=$channelId] Announced the following content:\n${content.joinToString("\n")}" }

        // search for media name and transport address
        require(content.single { it.startsWith("v=") } == "v=0") { "Only version 0 is supported" }
        val announced = content.filter { it.startsWith("m=") }
            .map { mediaAnnouncementString ->
                val d = mediaAnnouncementString.removePrefix("m=").split(" ")
                require(d.size >= 4) { "The Media Announcement `$mediaAnnouncementString` doesn't have all expected elements (>=4)." }
                val media = d[0]
                val (port, portNumber) = if (d[1].indexOf('/') < 0) {
                    listOf(d[1].toInt(), 1)
                } else {
                    d[1].split("/", limit = 2).map { it.toInt() }
                }
                val transport = d[2]
                val fmtList = d.subList(3, d.size).map { it.toInt() }
                require(fmtList.size == 1) { "fmtList != 1 is not supported" }
                val fmt = fmtList.first()
                require(fmt >= 96) { "Built in formats are not supported, only custom >= 96" }
                MediaAnnouncement(media, port, portNumber, transport, fmtList)
            }

        log.debug { "[channelId=$channelId] Announced media: $announced" }

        val rtpMappings = content.filter { it.startsWith("a=rtpmap:") }
            .map { rtpMapString ->
                val (payloadType, format) = rtpMapString.removePrefix("a=rtpmap:")
                    .split(" ", limit = 2)
                    .let { Pair(it[0].toInt(), it[1]) }
                val (encoding, clockRate, encodingParameters) = format.split("/")
                    .let { Triple(it[0], it[1].toInt(), if (it.size > 2) it[2] else null) }
                RtpMapping(payloadType, encoding, clockRate, encodingParameters)
            }
        log.debug { "[channelId=$channelId] RTP mappings: $rtpMappings" }

        channels.getValue(channelId).apply {
            this.announcements.addAll(announced)
            this.rtpMappings.addAll(rtpMappings)
            this.path = uri
        }
    }

    private fun doTearDown(sessionId: Long) {
        val session = sessions.remove(sessionId) ?: return
        rtspRecordReceiverHandler.unregisterSession(session.nettyChannelId)
        val mapping = checkNotNull(session.channelMappings.get(session.dataChannel)) { "Can't determine mapping for session=$session" }
        log.info { "Finished buffered streaming. Tearing down with mapping=$mapping..." }
        session.tearDownHandler.invoke(sessionId, mapping)

    }
}