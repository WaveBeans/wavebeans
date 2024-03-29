package io.wavebeans.http

import io.grpc.Server
import io.grpc.ServerBuilder
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.wavebeans.lib.table.TableRegistry
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.TimeUnit

class WbHttpService(
    private val serverPort: Int = 8080,
    private val communicatorPort: Int? = null,
    private val gracePeriodMillis: Long = 5000,
    private val tableRegistry: TableRegistry = TableRegistry.default,
    private val customHandlers: List<WbNettyHandler> = emptyList()
) : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val handlers: List<WbNettyHandler> by lazy {
        listOf(
            JavalinServletHandler(
                DefaultJavalinApp(
                    listOf(
                        { it.tableService(tableRegistry) },
                        { it.audioService(tableRegistry) }
                    )
                )
            )
        ) + customHandlers
    }
    private val bossGroup: EventLoopGroup = NioEventLoopGroup()
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()
    private var server: ChannelFuture? = null
    private var communicatorServer: Server? = null

    fun start(andWait: Boolean = false): WbHttpService {
        startCommunicator()
        startNetty(andWait)
        return this
    }

    private fun startCommunicator() {
        communicatorServer = communicatorPort?.let { port ->
            log.info { "Starting HTTP Communicator on port $port" }
            ServerBuilder.forPort(port)
                .addService(HttpCommunicatorService.instance(tableRegistry))
                .build()
                .start()
        }
    }

    private fun startNetty(andWait: Boolean) {
        handlers.forEach {
            log.debug { "Initializing $it" }
            it.init()
        }

        ServerBootstrap().apply {
            group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {

                    override fun initChannel(ch: SocketChannel) {
                        handlers.forEach {
                            log.debug { "Initiating channel $ch $it" }
                            it.initChannel(ch)
                        }
                        ch.pipeline().addFirst(LoggingHandler(WbHttpService::class.java, LogLevel.TRACE))
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            server = bind(serverPort).apply {
                if (andWait) {
                    channel().closeFuture().sync()
                }
            }

        }

    }

    override fun close() {
        log.info { "Closing..." }
        handlers.forEach { it.close() }
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
        server?.apply {
            log.info { "Stopping HTTP Service on port $serverPort..." }
            channel()
                .takeIf { !it.closeFuture().await(gracePeriodMillis) }
                ?.apply { close() }
        }
        communicatorServer?.apply {
            log.info { "Stopping HTTP Communicator Service on port $communicatorPort..." }
            if (!shutdown().awaitTermination(gracePeriodMillis, TimeUnit.MILLISECONDS)) {
                shutdownNow()
            }
        }
        log.info { "Closed" }
    }
}
