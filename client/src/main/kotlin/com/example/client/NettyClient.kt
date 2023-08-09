package com.example.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.logging.LoggingHandler
import mu.KotlinLogging
import java.net.URI


class NettyClient {
    fun connect(host: String, port: Int) {
        val uri = URI("ws://127.0.0.1:8080/chat")
        val scheme = if (uri.scheme == null) "ws" else uri.scheme
        val handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            uri, WebSocketVersion.V13, null, true, null
        )

        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        try {
            val b = Bootstrap() // (1)
            b.group(workerGroup) // (2)
            b.channel(NioSocketChannel::class.java) // (3)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        LoggingHandler(),
                        HttpClientCodec(),
                        HttpObjectAggregator(8192),
                        WebSocketClientHandler(handshaker),
                    )
                }
            })

            // Start the client.
            val f: ChannelFuture = b.connect(host, port).sync() // (5)

            // Wait until the connection is closed.
            f.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
        }
    }
}

class ClientHandler : SimpleChannelInboundHandler<TextWebSocketFrame>() {
    private val log = KotlinLogging.logger {}

    override fun channelActive(ctx: ChannelHandlerContext) {
        log.debug { "client active" }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
        log.debug { "client read" }
        log.debug { msg.text() }
    }
}
