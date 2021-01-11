package io.wavebeans.http

import io.javalin.Javalin
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.*
import mu.KotlinLogging
import javax.servlet.http.HttpServlet

@ChannelHandler.Sharable
class JavalinServletHandler(
    private val javalinApp: JavalinApp,
    private val requestBufferSize: Int = 1 /*Mb*/ * 1024 * 1024 * 1024
) : ChannelInboundHandlerAdapter(), WbNettyHandler {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val javalin by lazy {
        Javalin.createStandalone()
    }
    private val javalinServlet by lazy {
        javalin.servlet() as HttpServlet
    }

    override fun init() {
        javalinApp.setUp(javalin)
        javalinServlet.init()
    }

    override fun attachTo(pipeline: ChannelPipeline) {
        pipeline
            .addLast("HTTP decompressor", HttpContentDecompressor())
            .addLast("HTTP request decoder", HttpRequestDecoder())
            .addLast("HTTP object aggregator", HttpObjectAggregator(requestBufferSize))
            .addLast("HTTP response encoder", HttpResponseEncoder())
            .addLast("Javalin Servlet", this)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        log.debug { "Channel read ctx=$ctx, msg=$msg" }
        if (msg is FullHttpRequest) {
            try {
                val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                val reqAdapter = NettyHttpServletRequestAdapter(msg)
                NettyHttpServletResponseAdapter(response).use { resAdapter ->
                    javalinServlet.service(reqAdapter, resAdapter)
                    resAdapter.setContentLength(resAdapter.byteBuffer.writerIndex())
                    val output = response.replace(resAdapter.byteBuffer)
                    log.debug { "Javalin servlet finished serving with result $output" }
                    ctx.writeAndFlush(output)
                }
                msg.release()
                ctx.fireChannelReadComplete()
            } catch (e: Exception) {
                log.error(e) { "exception thrown ctx=$ctx, msg=$msg" }
            }
        }
    }

    override fun close() {
    }
}