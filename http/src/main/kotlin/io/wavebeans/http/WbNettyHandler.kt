package io.wavebeans.http

import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import java.io.Closeable

/**
 * Handler to extend the Netty implementation of [WbHttpService].
 */
interface WbNettyHandler : ChannelInboundHandler, Closeable {

    fun init()

    fun initChannel(channel: SocketChannel)
}