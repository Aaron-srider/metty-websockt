package com.example.server

import com.alibaba.fastjson2.JSONObject
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
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
import org.springframework.stereotype.Component
import java.net.InetSocketAddress

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

class Guest {

    var channel: Channel? = null

    var id: String? = null

    var tempName: String? = null

    var ip: String? = null

    override fun toString(): String {
        return "GuestImpl(id=$id, tempName=$tempName, ip=$ip)"
    }
}

object Room {

    var map: MutableMap<String, MutableMap<String, Guest>> = HashMap()

}

@Component
class ServerHandler : SimpleChannelInboundHandler<TextWebSocketFrame>() {

    private val log = KotlinLogging.logger {}

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

    override fun channelInactive(ctx: ChannelHandlerContext) {

        var a = ctx.channel().remoteAddress() as InetSocketAddress
        var remoteIp = a.address.hostAddress
        var id = ctx.channel().id().toString()
        log.debug {
            "leaving: ${remoteIp}, ${id}"
        }

        var map = Room.map
        var e = map.get(remoteIp)
        if (e == null) {
            log.debug { "no such room" }
        } else {
            var ee = e.get(id)
            if (ee != null) {
                log.debug { "leaving: ${ee}" }
                ee.channel?.close()
                e.remove(id)
                e.forEach {
                    var g = it.value.channel
                    if (g != null) {
                        g.writeAndFlush(
                            TextWebSocketFrame(JSONObject.toJSONString(
                                ProtoMessage().apply {
                                    this.messageType = "LEAVE"
                                    this.message = JSONObject.toJSONString(
                                        object {
                                            var id = id
                                        }
                                    )
                                }
                            )))
                    }
                    log.debug { "notify roommate for leaving: ${it.value}" }
                }
            } else {
                log.debug { "no such guest" }
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {

        log.debug { "server read: ${msg.text()}" }
        val remoteAddress = ctx.channel().remoteAddress() as InetSocketAddress
        var remoteIp = remoteAddress.address.hostAddress
        val protoMessage = JSONObject.parseObject(msg.text(), ProtoMessage::class.java)
        if (protoMessage.messageType == ProtoMessageType.HELLO.name) {

            var a =
                ctx.channel().remoteAddress() as InetSocketAddress

            var ip = a.address.toString()
            log.debug { "user connected, ip: ${ip}" }

            var msgBody = protoMessage.message
            var protoMessageEntity: Hello = JSONObject.parseObject(msgBody, Hello::class.java)
            log.debug { "Client HELLO: ${protoMessageEntity}" }


            var newGuest = Guest().apply {
                this.id = ctx.channel().id().toString()
                this.tempName = protoMessageEntity.tempName
                this.ip = remoteIp
                this.channel = ctx.channel()
            }

            log.debug { "Welcome new guest ${newGuest} to the room: ${remoteIp}" }
            // room.put(remoteIp, newGuest)

            var map = Room.map
            var e = map.get(remoteIp)
            if (e == null) {
                e = mutableMapOf()
                map.put(remoteIp, e)
            }

            log.debug { "roommates: " }
            e.forEach {
                log.debug { it.value }
            }

            e.forEach {
                var g = it.value.channel
                if (g != null) {
                    g.writeAndFlush(
                        TextWebSocketFrame(
                            JSONObject.toJSONString(

                                ProtoMessage().apply {
                                    this.messageType = "HELLO"
                                    this.message = msg.text()
                                })
                        )
                    )
                }
                log.debug { "notify roommate for coming: ${it.value}" }
            }
            e.put(newGuest.id!!, newGuest)

            val toMutableList = e.values.map {
                HelloBack().apply {
                    this.id = it.id
                    this.tempName = it.tempName
                }
            }

            var r = ProtoMessage().apply {
                this.messageType = "HELLO_BACK"
                this.message = JSONObject.toJSONString(
                    toMutableList
                )
            }
            ctx.channel().writeAndFlush(TextWebSocketFrame(JSONObject.toJSONString(r)))

            // room.reportSituation()
        } else if (protoMessage.messageType == "CHAT") {
            log.debug { "CHAT" }

            var msgBody = protoMessage.message
            var protoMessageEntity: Chat = JSONObject.parseObject(msgBody, Chat::class.java)

            log.debug { "from ${protoMessageEntity.fromId} to ${protoMessageEntity.toId} msg: ${protoMessageEntity.msg}" }

            var map = Room.map
            var e = map.get(protoMessageEntity.toId)
            if (e == null) {
                log.debug { "no such room" }
            } else {
                var to = e.get(protoMessageEntity.toId)
                if (to != null) {
                    ctx.channel().writeAndFlush(TextWebSocketFrame(msg.text()))
                }
            }
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

class HelloBack {
    var id: String? = null
    var tempName: String? = null
}

class Chat {
    var msg: String? = null
    var fromId: String? = null
    var toId: String? = null
}



