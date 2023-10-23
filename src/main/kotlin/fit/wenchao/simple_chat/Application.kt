package fit.wenchao.simple_chat

import cn.hutool.core.thread.ThreadUtil
import fit.wenchao.simple_chat.server.NettyServer
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Component
class TestRunner : CommandLineRunner {

    private val log = KotlinLogging.logger {}

    @Value("\${socket.server.address}")
    lateinit var address: String

    @Value("\${socket.server.port}")
    lateinit var port: String

    override fun run(vararg args: String?) {
        log.info { "write some here" }
        ThreadUtil.execAsync { NettyServer(address, port.toInt()).run() }
    }

}
