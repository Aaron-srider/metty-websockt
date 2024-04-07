package fit.wenchao.simple_chat.server

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
        val bossGroup: EventLoopGroup = NioEventLoopGroup(CustomThreadFactory("belg-")) // (1)
        val workerGroup: EventLoopGroup = NioEventLoopGroup(CustomThreadFactory("welg-"))
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
                                // LoggingHandler(),
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

data class Guest(
    var device: String? = null,
    var channel: Channel? = null,
    var uid: String? = null,
    var tempName: String? = null,
    var ip: String? = null,
) {
}

object Room {
    private val log = KotlinLogging.logger {}

    init {
        Thread {
            while (true) {
                Thread.sleep(1000 * 2)
                map.forEach {
                    log.debug { "room: ${it.key}" }
                    it.value.forEach {
                        log.debug { "guest: ${it.value}" }
                    }
                }
            }
        }.start()
    }

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
        var channel_id = ctx.channel().id().toString()
        log.debug {
            "leaving: ${remoteIp}"
        }

        var map = Room.map
        var e = map.get(remoteIp)
        if (e == null) {
            log.debug { "no such room" }
        } else {
            var ee = e.get(channel_id)
            if (ee != null) {
                log.debug { "leaving: ${ee}" }
                ee.channel?.close()
                e.remove(channel_id)
                e.forEach {
                    var g = it.value.channel
                    if (g != null) {
                        g.writeAndFlush(
                            TextWebSocketFrame(JSONObject.toJSONString(
                                ProtoMessage().apply {
                                    this.messageType = "LEAVE"
                                    this.message = JSONObject.toJSONString(
                                        object {
                                            var uid = ee.uid
                                        }
                                    )
                                }
                            )))
                        log.debug { "notify roommate for leaving: ${it.value}" }
                    }
                }
            } else {
                log.debug { "no such guest" }
            }
        }
    }

    var backMap = mutableMapOf<String, String>()
    override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {

        var remoteIp = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
        val protoMessage = JSONObject.parseObject(msg.text(), ProtoMessage::class.java)
        if (protoMessage.messageType == ProtoMessageType.HELLO.name) {
            var a =
                ctx.channel().remoteAddress() as InetSocketAddress

            var ip = a.address.toString()
            log.debug { "user connected, ip: ${ip}" }

            var msgBody = protoMessage.message
            var protoMessageEntity: Hello = JSONObject.parseObject(msgBody, Hello::class.java)
            log.debug { "Client HELLO: ${protoMessageEntity}" }

            var guestId = protoMessageEntity.uid!!

            var newGuest = Guest().apply {
                this.uid = guestId
                this.tempName = protoMessageEntity.tempName!!
                this.ip = remoteIp
                this.device = protoMessageEntity.osFamily
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
                                    this.message = JSONObject.toJSONString(
                                        object {
                                            var uid = newGuest.uid
                                            var tempName = newGuest.tempName
                                            var device = newGuest.device
                                        }
                                    )
                                })
                        )
                    )
                }
                log.debug { "notify roommate for coming: ${it.value}" }
            }


            val toMutableList = e.values.map {
                object {
                    var uid = it.uid
                    var tempName = it.tempName
                    var device = it.device
                }
            }

            var me = object {
                var uid = newGuest.uid
                var tempName = newGuest.tempName
                var device = newGuest.device
            }

            var r = ProtoMessage().apply {
                this.messageType = "HELLO_BACK"
                this.message = JSONObject.toJSONString(
                    object {
                        var user_list = toMutableList
                        var me = me
                    }
                )
            }
            ctx.channel().writeAndFlush(TextWebSocketFrame(JSONObject.toJSONString(r)))
            e.put(ctx.channel().id().toString(), newGuest)

            Thread {

                while (true) {
                    if (!(ctx.channel().isActive && ctx.channel().isOpen)) {
                        ctx.channel().close()
                        break
                    }

                    Thread.sleep(2000)

                    var iiid = "${ctx.channel().id()} - ${remoteIp}"
                    // log.debug { "Send alive ping to: ${iiid}" }
                    ctx.channel().writeAndFlush(TextWebSocketFrame(JSONObject.toJSONString(
                        object {
                            var messageType = "ALIVE_PING"
                        }
                    ))).addListener {
                        if (it.isSuccess) {
                            Thread {
                                // log.debug { "Timeout 1s for ack" }
                                Thread.sleep(1000)
                                var i = backMap.containsKey(ctx.channel().id().toString())
                                if (!i) {
                                    // log.debug { "No ping ack received after 1s timeout, close the channel: ${iiid}" }
                                    ctx.channel().close()
                                }
                                backMap.remove(ctx.channel().id().toString())
                            }.start()
                        }
                    }
                }
            }.start()

            // room.reportSituation()
        } else if (protoMessage.messageType == "ALIVE_ACK") {
            backMap.put(ctx.channel().id().toString(), "")
        } else if (protoMessage.messageType == "MESSAGE") {
            log.debug { "MESSAGE" }

            var msgBody = protoMessage.message
            var protoMessageEntity: Chat = JSONObject.parseObject(msgBody, Chat::class.java)

            log.debug { "from ${protoMessageEntity.fromUid} to ${protoMessageEntity.toUid} msg: ${protoMessageEntity.msg}" }

            var map = Room.map
            var room = map.get(remoteIp)
            if (room == null) {
                log.debug { "no such room" }
            } else {
                room.values.find { it.uid == protoMessageEntity.toUid }?.let {
                    it.channel!!.writeAndFlush(TextWebSocketFrame(msg.text()))
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
    var uid: String? = null
    var tempName: String? = null
    var osFamily: String? = null
    var osArch: String? = null
    var ua: String? = null

    override fun toString(): String {
        return "Hello(uid=$uid, tempName=$tempName, osFamily=$osFamily, osArch=$osArch, ua=$ua)"
    }
}

class Chat {
    var msg: String? = null
    var fromUid: String? = null
    var toUid: String? = null
}



