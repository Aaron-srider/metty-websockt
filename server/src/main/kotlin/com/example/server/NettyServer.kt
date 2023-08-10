package com.example.server

import com.alibaba.fastjson2.JSONObject
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.ServerHandshakeStateEvent
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.stream.ChunkedWriteHandler
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.InetSocketAddress
import java.util.UUID

/**
 * Discards any incoming data.
 */
class NettyServer(private val host: String, private val port: Int) {
    private val log = KotlinLogging.logger {}
    fun run() {
        val bossGroup: EventLoopGroup = NioEventLoopGroup() // (1)
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap() // (2)
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java) // (3)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline()
                            .addLast(HttpServerCodec())
                            .addLast(ChunkedWriteHandler())
                            .addLast(HttpObjectAggregator(8192))
                            .addLast(WebSocketServerProtocolHandler("/" + "chat"))
                            .addLast(
                                LoggingHandler(),
                                ServerHandler()
                            )
                    }
                })

            // Bind and start to accept incoming connections.
            b.bind(host, port).addListener {
                if (it.isSuccess) {
                    println("server started")
                } else {
                    println("server start failed")
                    workerGroup.shutdownGracefully()
                    bossGroup.shutdownGracefully()
                }
            }
        } catch (e: Exception) {
            log.debug { "server start failed" }
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }
}

@Component
class ServerHandler : SimpleChannelInboundHandler<TextWebSocketFrame>() {

    @Autowired
    lateinit var room: Room

    private val log = KotlinLogging.logger {}

    @Throws(java.lang.Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is ServerHandshakeStateEvent
            && evt == ServerHandshakeStateEvent.HANDSHAKE_COMPLETE
        ) {

            // Perform actions after WebSocket handshake is completed
            log.debug { "WebSocket handshake completed: " + ctx.channel().remoteAddress() }
            ctx.channel().writeAndFlush(TextWebSocketFrame("Welcome to the WebSocket server!"))
        }
        super.userEventTriggered(ctx, evt)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        log.debug { "connection in: ${ctx.channel().remoteAddress()}" }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
        log.debug { "server read: ${msg.text()}" }
        val protoMessage = JSONObject.parseObject(msg.text(), ProtoMessage::class.java)
        if (protoMessage.messageType == ProtoMessageType.HELLO.name) {
            var protoMessage = protoMessage.message
            var protoMessageEntity: Hello = JSONObject.parseObject(protoMessage, Hello::class.java)
            log.debug { "Client HELLO: ${protoMessageEntity}" }

            val remoteAddress = ctx.channel().remoteAddress() as InetSocketAddress
            var remoteIp = remoteAddress.address.hostAddress

            var newGuest = GuestImpl().apply {
                this.id = UUID.randomUUID().toString()
                this.tempName = protoMessageEntity.tempName
                this.ip = remoteIp
            }

            log.debug { "Welcome new guest ${newGuest} to the room: ${remoteIp}" }
            room.put(remoteIp, newGuest)


            room.reportSituation()
        } else {
            log.warn { "Server can not proccess this kind of msg: ${protoMessage.messageType}" }
        }
    }


}

enum class ProtoMessageType {
    HELLO
}

class ProtoMessage {
    var messageType: String? = null
    var message: String? = null
}


class Hello {
    var tempName: String? = null
    var osFamily: String? = null
    var osArch: String? = null
    var ua: String? = null

    override fun toString(): String {
        return "Hello(tempName=$tempName, osFamily=$osFamily, osArch=$osArch, ua=$ua)"
    }
}


