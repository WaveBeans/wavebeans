package io.wavebeans.http

import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelPipeline
import java.io.Closeable

interface WbNettyHandler : ChannelInboundHandler, Closeable {
    fun init()

    fun attachTo(pipeline: ChannelPipeline)
}